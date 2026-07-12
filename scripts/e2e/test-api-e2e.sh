#!/usr/bin/env bash
# End-to-end API smoke test for aionn-modulith-backend.
# Covers only endpoints that need Postgres + Redis (no Sumsub / Google / GHN /
# Cloudinary / SMS / email). Run against an app started with the mock providers
# enabled — see scripts/e2e/README.md.
#
# Exit non-zero on the first failed step so CI can gate on it.

set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"

# Force using Windows native curl.exe to avoid MSYS2 loopback connection blocks
curl() {
    curl.exe "$@"
}

# --- helpers ------------------------------------------------------------------
COLOR_RESET=$'\033[0m'
COLOR_OK=$'\033[32m'
COLOR_ERR=$'\033[31m'
COLOR_INFO=$'\033[36m'

step()  { printf '\n%s==> %s%s\n' "$COLOR_INFO" "$*" "$COLOR_RESET"; }
ok()    { printf '%s   OK  %s%s\n'  "$COLOR_OK"  "$*" "$COLOR_RESET"; }
fail()  { printf '%s   FAIL %s%s\n' "$COLOR_ERR" "$*" "$COLOR_RESET" >&2; exit 1; }

# Extract JSON field with plain grep+sed — avoids jq dependency on Windows.
# Usage: json_field '"regId":"abc"' regId  →  abc
json_field() {
    local body=$1 key=$2
    printf '%s' "$body" \
        | grep -oE "\"$key\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" \
        | head -1 \
        | sed -E "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"([^\"]*)\".*/\1/"
}

json_number() {
    local body=$1 key=$2
    printf '%s' "$body" \
        | grep -oE "\"$key\"[[:space:]]*:[[:space:]]*[0-9]+" \
        | head -1 \
        | sed -E "s/.*\"$key\"[[:space:]]*:[[:space:]]*([0-9]+).*/\1/"
}

# All requests go through this so we can assert HTTP status in one place.
# Prints the JSON body on stdout. Exits on unexpected status.
# Usage: http_call METHOD PATH EXPECTED_STATUS [BEARER] [BODY]
http_call() {
    local method=$1 path=$2 expected=$3 bearer=${4:-} body=${5:-}
    local url="$BASE_URL$path"
    local hdr=(-H 'Accept: application/json')
    [ -n "$body" ]   && hdr+=(-H 'Content-Type: application/json')
    [ -n "$bearer" ] && hdr+=(-H "Authorization: Bearer $bearer")
    hdr+=(-H "X-Client-Type: mobile")

    local response status resp_body
    if [ -n "$body" ]; then
        response=$(curl --noproxy "*" -sS -o - -w '\n<<<STATUS>>>%{http_code}' \
            -X "$method" "${hdr[@]}" --data "$body" "$url") || fail "curl $method $path"
    else
        response=$(curl --noproxy "*" -sS -o - -w '\n<<<STATUS>>>%{http_code}' \
            -X "$method" "${hdr[@]}" "$url") || fail "curl $method $path"
    fi
    status=${response##*<<<STATUS>>>}
    resp_body=${response%$'\n'<<<STATUS>>>*}
    if [ "$status" != "$expected" ]; then
        fail "$method $path expected $expected got $status. Body: $resp_body"
    fi
    printf '%s' "$resp_body"
}

# --- prerequisites ------------------------------------------------------------
step "Waiting for $BASE_URL/actuator/health"
for i in $(seq 1 90); do
    response=$(curl --noproxy "*" -sS "$BASE_URL/actuator/health" 2>&1 || true)
    if echo "$response" | grep -q '"UP"'; then
        ok "app is UP"
        break
    fi
    if [ "$i" = 90 ]; then
        fail "app did not become healthy after 90s. Is it running with mock providers?"
    fi
    sleep 1
done

# --- 1. Public geography (read-only, no auth) --------------------------------
step "1. Geography — public read"
countries=$(http_call GET /api/v1/geography/countries 200)
country_code=$(json_field "$countries" "code")
[ -n "$country_code" ] || fail "countries list is empty — did you seed geography?"
ok "countries[0].code=$country_code"
http_call GET "/api/v1/geography/provinces?countryCode=$country_code" 200 >/dev/null
ok "provinces?countryCode=$country_code returns 200"

# --- 2. Registration flow (initiate → verify → complete) ---------------------
step "2. Registration flow"
SUFFIX=$(date +%s)
PHONE_LOCAL="09$(printf '%08d' $((RANDOM * 32768 + RANDOM)) | cut -c1-8)"
USERNAME="e2e_user_$SUFFIX"
PASSWORD='E2eTest123!'

initiate_body="{\"phoneNumber\":\"$PHONE_LOCAL\",\"captchaToken\":\"any\"}"
initiate_resp=$(http_call POST /api/v1/registrations/initiate 201 "" "$initiate_body")
REG_ID=$(json_field "$initiate_resp" "regId")
OTP_CODE=$(json_field "$initiate_resp" "otpCode")
[ -n "$REG_ID" ]   || fail "regId missing from initiate response: $initiate_resp"
[ -n "$OTP_CODE" ] || fail "otpCode missing — check twilio.enabled=false so OTP is exposed"
ok "regId=$REG_ID  otpCode=$OTP_CODE"

verify_body="{\"otpCode\":\"$OTP_CODE\"}"
verify_resp=$(http_call POST "/api/v1/registrations/$REG_ID/verify-otp" 200 "" "$verify_body")
VERIFICATION_TOKEN=$(json_field "$verify_resp" "verificationToken")
[ -n "$VERIFICATION_TOKEN" ] || fail "verificationToken missing: $verify_resp"
ok "verificationToken acquired"

complete_body="{\"password\":\"$PASSWORD\",\"username\":\"$USERNAME\",\"verificationToken\":\"$VERIFICATION_TOKEN\"}"
complete_resp=$(http_call POST "/api/v1/registrations/$REG_ID/complete" 200 "" "$complete_body")
USER_ID=$(json_field "$complete_resp" "userId")
ACCESS_TOKEN=$(json_field "$complete_resp" "accessToken")
REFRESH_TOKEN=$(json_field "$complete_resp" "refreshToken")
SESSION_ID=$(json_field "$complete_resp" "sessionId")
[ -n "$ACCESS_TOKEN" ]  || fail "accessToken missing from complete response"
[ -n "$REFRESH_TOKEN" ] || fail "refreshToken missing from complete response"
ok "registered userId=$USER_ID sessionId=$SESSION_ID"

# --- 3. Timestamp shape check — must be Instant ISO-8601 with Z --------------
step "3. Timestamp format — response envelope 'timestamp' must be ISO-8601 UTC"
ts=$(json_field "$complete_resp" "timestamp")
[ -n "$ts" ] || fail "response has no timestamp"
case "$ts" in
    *Z) ok "timestamp=$ts (ends with Z as expected)" ;;
    *)  fail "timestamp=$ts does not end with 'Z' — Instant serialisation regression?" ;;
esac

# --- 4. Auth: refresh, sessions, logout of one, keep working -----------------
step "4. Auth flow"
refresh_body="{\"refreshToken\":\"$REFRESH_TOKEN\"}"
refresh_resp=$(http_call POST /api/v1/auth/refresh 200 "" "$refresh_body")
NEW_ACCESS=$(json_field "$refresh_resp" "accessToken")
NEW_REFRESH=$(json_field "$refresh_resp" "refreshToken")
[ -n "$NEW_ACCESS" ]  || fail "refresh returned no accessToken"
[ -n "$NEW_REFRESH" ] || fail "refresh returned no refreshToken"
ACCESS_TOKEN=$NEW_ACCESS
REFRESH_TOKEN=$NEW_REFRESH
ok "tokens rotated"

sessions=$(http_call GET /api/v1/auth/sessions 200 "$ACCESS_TOKEN")
first_session=$(json_field "$sessions" "sessionId")
[ -n "$first_session" ] || fail "sessions list empty"
ok "sessions has entries (first=$first_session)"

# --- 5. Profile CRUD ---------------------------------------------------------
step "5. Profile — GET /me, update display name"
me=$(http_call GET /api/v1/users/me 200 "$ACCESS_TOKEN")
me_user=$(json_field "$me" "userId")
[ "$me_user" = "$USER_ID" ] || fail "GET /me userId mismatch: got $me_user want $USER_ID"
ok "GET /me returned own userId"

http_call PATCH /api/v1/users/me/display-name 200 "$ACCESS_TOKEN" \
    "{\"displayName\":\"E2E User $SUFFIX\"}" >/dev/null
ok "display name updated"

# --- 6. Address CRUD --------------------------------------------------------
step "6. Address CRUD"
provinces=$(http_call GET "/api/v1/geography/provinces?countryCode=$country_code" 200)
PROV_CODE=$(json_field "$provinces" "code")
[ -n "$PROV_CODE" ] || fail "no provinces for $country_code"

districts=$(http_call GET "/api/v1/geography/districts?provinceCode=$PROV_CODE" 200)
DIST_CODE=$(json_field "$districts" "code")
[ -n "$DIST_CODE" ] || fail "no districts for $PROV_CODE"

wards=$(http_call GET "/api/v1/geography/wards?districtCode=$DIST_CODE" 200)
WARD_CODE=$(json_field "$wards" "code")
[ -n "$WARD_CODE" ] || fail "no wards for $DIST_CODE"
ok "geography drilled: province=$PROV_CODE district=$DIST_CODE ward=$WARD_CODE"

addr_body=$(cat <<EOF
{
  "contactName": "E2E Recipient",
  "phone": "$PHONE_LOCAL",
  "provinceCode": "$PROV_CODE",
  "districtCode": "$DIST_CODE",
  "wardCode": "$WARD_CODE",
  "detailAddress": "123 E2E Street",
  "type": "HOME",
  "isDefault": true
}
EOF
)
addr_resp=$(http_call POST /api/v1/addresses 201 "$ACCESS_TOKEN" "$addr_body")
ADDR_ID=$(json_field "$addr_resp" "addressId")
[ -n "$ADDR_ID" ] || fail "addressId missing from create response: $addr_resp"
ok "address created id=$ADDR_ID"

http_call GET /api/v1/addresses 200 "$ACCESS_TOKEN" >/dev/null
ok "GET /api/v1/addresses returned 200"

# --- 7. Consent + preference -------------------------------------------------
step "7. Consent & preference"
http_call POST /api/v1/consents/terms 200 "$ACCESS_TOKEN" '{"version":"1.0"}' >/dev/null
ok "terms consent recorded"
http_call PATCH /api/v1/consents/marketing 200 "$ACCESS_TOKEN" '{"subscribed":true}' >/dev/null
ok "marketing consent updated"
http_call GET /api/v1/preferences 200 "$ACCESS_TOKEN" >/dev/null
ok "preferences GET works"

# --- 8. Feedback submission --------------------------------------------------
step "8. Feedback"
fb_body='{"category":"GENERAL","subject":"e2e","content":"end-to-end smoke test feedback","rating":5}'
http_call POST /api/v1/feedbacks 200 "$ACCESS_TOKEN" "$fb_body" >/dev/null
ok "feedback submitted"
http_call GET /api/v1/feedbacks/me 200 "$ACCESS_TOKEN" >/dev/null
ok "GET /feedbacks/me returned 200"

# --- 9. Catalog: public browsing --------------------------------------------
step "9. Catalog — public read endpoints"
http_call GET '/api/v1/catalog/brands?page=0&size=10' 200 >/dev/null
ok "GET brands"
http_call GET /api/v1/catalog/categories/roots 200 >/dev/null
ok "GET categories/roots"
http_call GET /api/v1/catalog/categories/tree 200 >/dev/null
ok "GET categories/tree"
http_call GET '/api/v1/catalog/products?page=0&size=10' 200 >/dev/null
ok "GET products"
http_call GET '/api/v1/catalog/products/search?keyword=phone' 200 >/dev/null
ok "GET products/search"

# --- 10. Merchant registration (JWT) -----------------------------------------
step "10. Merchant registration"
merchant_body="{\"name\":\"E2E Merchant $SUFFIX\"}"
merchant_resp=$(http_call POST /api/v1/catalog/merchants 201 "$ACCESS_TOKEN" "$merchant_body")
MERCHANT_ID=$(json_field "$merchant_resp" "merchantId")
[ -n "$MERCHANT_ID" ] || fail "merchantId missing: $merchant_resp"
ok "merchant registered id=$MERCHANT_ID"

http_call GET /api/v1/catalog/merchants/me 200 "$ACCESS_TOKEN" >/dev/null
ok "GET merchants/me returned 200"

# --- 11. Auth cleanup: logout current session --------------------------------
step "11. Logout"
http_call POST /api/v1/auth/logout 200 "$ACCESS_TOKEN" >/dev/null
ok "logout returned 200"

printf '\n%sAll e2e checks passed.%s\n' "$COLOR_OK" "$COLOR_RESET"
