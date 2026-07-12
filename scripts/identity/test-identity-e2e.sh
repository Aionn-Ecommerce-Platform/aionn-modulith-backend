#!/usr/bin/env bash
# E2E API smoke test for the Identity module.
# Exit non-zero on the first failed step so CI can gate on it.

set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"

# On MSYS2 / Git Bash on Windows, curl is wrapped and blocks loopback connections.
# Override with the native curl.exe to bypass that restriction.
if [[ "${OSTYPE:-}" == msys* || "${OSTYPE:-}" == cygwin* ]] || command -v curl.exe &>/dev/null; then
    curl() { curl.exe "$@"; }
fi

# --- helpers ------------------------------------------------------------------
COLOR_RESET=$'\033[0m'
COLOR_OK=$'\033[32m'
COLOR_ERR=$'\033[31m'
COLOR_INFO=$'\033[36m'

step()  { printf '\n%s==> %s%s\n' "$COLOR_INFO" "$*" "$COLOR_RESET"; }
ok()    { printf '%s   OK  %s%s\n'  "$COLOR_OK"  "$*" "$COLOR_RESET"; }
fail()  { printf '%s   FAIL %s%s\n' "$COLOR_ERR" "$*" "$COLOR_RESET" >&2; exit 1; }

json_field() {
    local body=$1 key=$2
    printf '%s' "$body" \
        | grep -oE "\"$key\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" \
        | head -1 \
        | sed -E "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"([^\"]*)\".*/\1/"
}

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
        fail "app did not become healthy after 90s."
    fi
    sleep 1
done

# --- 1. Public geography -----------------------------------------------------
step "1. Geography — public read"
countries=$(http_call GET /api/v1/geography/countries 200)
country_code=$(json_field "$countries" "code")
[ -n "$country_code" ] || fail "countries list is empty — did you seed geography?"
ok "countries[0].code=$country_code"
http_call GET "/api/v1/geography/provinces?countryCode=$country_code" 200 >/dev/null
ok "provinces?countryCode=$country_code returns 200"

# --- 2. Registration flow ----------------------------------------------------
step "2. Registration flow"
SUFFIX=$(date +%s)
PHONE_LOCAL="09$(printf '%08d' $((RANDOM * 32768 + RANDOM)) | cut -c1-8)"
USERNAME="e2e_user_$SUFFIX"
PASSWORD='E2eTest123!'

initiate_body="{\"phoneNumber\":\"$PHONE_LOCAL\",\"captchaToken\":\"any\"}"
initiate_resp=$(http_call POST /api/v1/registrations/initiate 201 "" "$initiate_body")
REG_ID=$(json_field "$initiate_resp" "regId")
OTP_CODE=$(json_field "$initiate_resp" "otpCode")
[ -n "$REG_ID" ]   || fail "regId missing from initiate response"
[ -n "$OTP_CODE" ] || fail "otpCode missing"
ok "regId=$REG_ID  otpCode=$OTP_CODE"

verify_body="{\"otpCode\":\"$OTP_CODE\"}"
verify_resp=$(http_call POST "/api/v1/registrations/$REG_ID/verify-otp" 200 "" "$verify_body")
VERIFICATION_TOKEN=$(json_field "$verify_resp" "verificationToken")
[ -n "$VERIFICATION_TOKEN" ] || fail "verificationToken missing"
ok "verificationToken acquired"

complete_body="{\"password\":\"$PASSWORD\",\"username\":\"$USERNAME\",\"verificationToken\":\"$VERIFICATION_TOKEN\"}"
complete_resp=$(http_call POST "/api/v1/registrations/$REG_ID/complete" 200 "" "$complete_body")
USER_ID=$(json_field "$complete_resp" "userId")
ACCESS_TOKEN=$(json_field "$complete_resp" "accessToken")
REFRESH_TOKEN=$(json_field "$complete_resp" "refreshToken")
SESSION_ID=$(json_field "$complete_resp" "sessionId")
[ -n "$ACCESS_TOKEN" ]  || fail "accessToken missing"
ok "registered userId=$USER_ID"

# --- 3. Auth flow -------------------------------------------------------------
step "3. Auth flow"
refresh_body="{\"refreshToken\":\"$REFRESH_TOKEN\"}"
refresh_resp=$(http_call POST /api/v1/auth/refresh 200 "" "$refresh_body")
NEW_ACCESS=$(json_field "$refresh_resp" "accessToken")
[ -n "$NEW_ACCESS" ] || fail "refresh returned no accessToken"
ACCESS_TOKEN=$NEW_ACCESS
ok "tokens rotated"

sessions=$(http_call GET /api/v1/auth/sessions 200 "$ACCESS_TOKEN")
first_session=$(json_field "$sessions" "sessionId")
[ -n "$first_session" ] || fail "sessions list empty"
ok "sessions active"

# --- 4. Profile CRUD ----------------------------------------------------------
step "4. Profile — GET /me, update display name"
me=$(http_call GET /api/v1/users/me 200 "$ACCESS_TOKEN")
me_user=$(json_field "$me" "userId")
[ "$me_user" = "$USER_ID" ] || fail "userId mismatch"
ok "GET /me returned own userId"

http_call PATCH /api/v1/users/me/display-name 200 "$ACCESS_TOKEN" "{\"displayName\":\"E2E User $SUFFIX\"}" >/dev/null
ok "display name updated"

# --- 5. Address CRUD ----------------------------------------------------------
step "5. Address CRUD"
provinces=$(http_call GET "/api/v1/geography/provinces?countryCode=$country_code" 200)
PROV_CODE=$(json_field "$provinces" "code")
districts=$(http_call GET "/api/v1/geography/districts?provinceCode=$PROV_CODE" 200)
DIST_CODE=$(json_field "$districts" "code")
wards=$(http_call GET "/api/v1/geography/wards?districtCode=$DIST_CODE" 200)
WARD_CODE=$(json_field "$wards" "code")
ok "geography drilled"

addr_body="{\"contactName\":\"E2E Recipient\",\"phone\":\"$PHONE_LOCAL\",\"provinceCode\":\"$PROV_CODE\",\"districtCode\":\"$DIST_CODE\",\"wardCode\":\"$WARD_CODE\",\"detailAddress\":\"123 E2E Street\",\"type\":\"HOME\",\"isDefault\":true}"
addr_resp=$(http_call POST /api/v1/addresses 201 "$ACCESS_TOKEN" "$addr_body")
ADDR_ID=$(json_field "$addr_resp" "addressId")
[ -n "$ADDR_ID" ] || fail "addressId missing"
ok "address created id=$ADDR_ID"

# --- 6. Consent & preference --------------------------------------------------
step "6. Consent & preference"
http_call POST /api/v1/consents/terms 200 "$ACCESS_TOKEN" '{"version":"1.0"}' >/dev/null
http_call PATCH /api/v1/consents/marketing 200 "$ACCESS_TOKEN" '{"subscribed":true}' >/dev/null
http_call GET /api/v1/preferences 200 "$ACCESS_TOKEN" >/dev/null
ok "consent & preferences verified"

# --- 7. Feedback -------------------------------------------------------------
step "7. Feedback"
fb_body='{"category":"GENERAL","subject":"e2e","content":"identity test","rating":5}'
http_call POST /api/v1/feedbacks 200 "$ACCESS_TOKEN" "$fb_body" >/dev/null
http_call GET /api/v1/feedbacks/me 200 "$ACCESS_TOKEN" >/dev/null
ok "feedback submitted"

# --- 8. Logout ----------------------------------------------------------------
step "8. Logout"
http_call POST /api/v1/auth/logout 200 "$ACCESS_TOKEN" >/dev/null
ok "logout returned 200"

printf '\n%sAll Identity E2E checks passed.%s\n' "$COLOR_OK" "$COLOR_RESET"
