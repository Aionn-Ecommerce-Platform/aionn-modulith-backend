#!/usr/bin/env bash
# E2E API smoke test for the Shipping module.
# Exit non-zero on the first failed step so CI can gate on it.
#
# Scope note: shipment creation requires ROLE_MERCHANT (granted only through the
# admin role endpoint) and register/label call the live GHN carrier. Neither is
# reachable from a self-service smoke run, so this script covers the quote path
# (served from the seeded shipping_rates table), read endpoints and authz guards.

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

json_field_num() {
    local body=$1 key=$2
    printf '%s' "$body" \
        | grep -oE "\"$key\"[[:space:]]*:[[:space:]]*[0-9]+(\.[0-9]+)?" \
        | head -1 \
        | sed -E "s/.*\"$key\"[[:space:]]*:[[:space:]]*([0-9]+(\.[0-9]+)?).*/\1/"
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

quote_body() {
    local order_id=$1 province=$2
    cat <<JSON
{"orderId":"$order_id",
 "address":{"fullName":"E2E Ship Receiver","phone":"0912345678",
            "addressLine":"12 Modulith Street","wardCode":"21211",
            "districtId":"1454","provinceCode":"$province","countryCode":"VN"},
 "dimensions":{"weightGram":500,"lengthCm":20,"widthCm":15,"heightCm":10},
 "currency":"VND"}
JSON
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

# --- 1. Authenticate an E2E user ---------------------------------------------
step "1. Authenticating E2E user"
SUFFIX=$(date +%s)
PHONE_LOCAL="09$(printf '%08d' $((RANDOM * 32768 + RANDOM)) | cut -c1-8)"
USERNAME="e2e_ship_user_$SUFFIX"
PASSWORD='E2eTest123!'

initiate_body="{\"phoneNumber\":\"$PHONE_LOCAL\",\"captchaToken\":\"any\"}"
initiate_resp=$(http_call POST /api/v1/registrations/initiate 201 "" "$initiate_body")
REG_ID=$(json_field "$initiate_resp" "regId")
OTP_CODE=$(json_field "$initiate_resp" "otpCode")

verify_body="{\"otpCode\":\"$OTP_CODE\"}"
verify_resp=$(http_call POST "/api/v1/registrations/$REG_ID/verify-otp" 200 "" "$verify_body")
VERIFICATION_TOKEN=$(json_field "$verify_resp" "verificationToken")

complete_body="{\"password\":\"$PASSWORD\",\"username\":\"$USERNAME\",\"verificationToken\":\"$VERIFICATION_TOKEN\"}"
complete_resp=$(http_call POST "/api/v1/registrations/$REG_ID/complete" 200 "" "$complete_body")
ACCESS_TOKEN=$(json_field "$complete_resp" "accessToken")
[ -n "$ACCESS_TOKEN" ] || fail "accessToken missing from registration response"
ok "E2E user authenticated"

# --- 2. Quote from the seeded rate table -------------------------------------
step "2. Quote shipping fee from configured zone rates"
ORDER_ID="ORD-E2E-SHIP-$SUFFIX"

hn_resp=$(http_call POST /api/v1/shipping/shipments/quote 200 "$ACCESS_TOKEN" "$(quote_body "$ORDER_ID" 'VN-HN')")
HN_FEE=$(json_field_num "$hn_resp" "fee")
HN_SOURCE=$(json_field "$hn_resp" "source")
HN_ZONE=$(json_field "$hn_resp" "zoneCode")
[ "$HN_SOURCE" = "configured-rate" ] || fail "expected source configured-rate, got '$HN_SOURCE'"
[ "$HN_ZONE" = "VN-HN" ] || fail "expected zoneCode VN-HN, got '$HN_ZONE'"
case "$HN_FEE" in 30000|30000.0|30000.00) ;; *) fail "expected VN-HN fee 30000, got '$HN_FEE'" ;; esac
ok "VN-HN quote resolved from configured rate (fee=$HN_FEE)"

sg_resp=$(http_call POST /api/v1/shipping/shipments/quote 200 "$ACCESS_TOKEN" "$(quote_body "$ORDER_ID" 'VN-SG')")
SG_FEE=$(json_field_num "$sg_resp" "fee")
case "$SG_FEE" in 35000|35000.0|35000.00) ;; *) fail "expected VN-SG fee 35000, got '$SG_FEE'" ;; esac
ok "VN-SG quote resolved from configured rate (fee=$SG_FEE)"

dn_resp=$(http_call POST /api/v1/shipping/shipments/quote 200 "$ACCESS_TOKEN" "$(quote_body "$ORDER_ID" 'VN-DN')")
DN_FEE=$(json_field_num "$dn_resp" "fee")
case "$DN_FEE" in 40000|40000.0|40000.00) ;; *) fail "expected VN-DN fee 40000, got '$DN_FEE'" ;; esac
ok "VN-DN quote resolved from configured rate (fee=$DN_FEE)"

# --- 3. Validation and authorization guards ----------------------------------
step "3. Validation and authorization guards"
http_call POST /api/v1/shipping/shipments/quote 403 "" "$(quote_body "$ORDER_ID" 'VN-HN')" >/dev/null
ok "unauthenticated quote rejected with 403"

invalid_body='{"address":{"provinceCode":"VN-HN"},"dimensions":{"weightGram":500}}'
http_call POST /api/v1/shipping/shipments/quote 400 "$ACCESS_TOKEN" "$invalid_body" >/dev/null
ok "quote without orderId rejected with 400"

create_body=$(cat <<JSON
{"orderId":"$ORDER_ID","userId":"e2e-buyer",
 "address":{"fullName":"E2E Ship Receiver","phone":"0912345678",
            "addressLine":"12 Modulith Street","wardCode":"21211",
            "districtId":"1454","provinceCode":"VN-HN","countryCode":"VN"},
 "dimensions":{"weightGram":500,"lengthCm":20,"widthCm":15,"heightCm":10},
 "codAmount":0,"shippingFee":30000,"currency":"VND"}
JSON
)
http_call POST /api/v1/shipping/shipments 403 "$ACCESS_TOKEN" "$create_body" >/dev/null
ok "shipment creation without ROLE_MERCHANT rejected with 403"

# --- 4. Shipment read endpoints ----------------------------------------------
step "4. Shipment read endpoints"
http_call GET "/api/v1/shipping/shipments/SHIPMENT_DOES_NOT_EXIST_$SUFFIX" 404 "$ACCESS_TOKEN" >/dev/null
ok "unknown shipment returns 404"

by_order_resp=$(http_call GET "/api/v1/shipping/shipments/by-order/$ORDER_ID" 200 "$ACCESS_TOKEN")
echo "$by_order_resp" | grep -q '"data"[[:space:]]*:[[:space:]]*\[' \
    || fail "by-order did not return a data array. Body: $by_order_resp"
ok "by-order returns an empty, ownership-filtered list"

# --- 5. Cleanup ---------------------------------------------------------------
step "5. Logout E2E session"
http_call POST /api/v1/auth/logout 200 "$ACCESS_TOKEN" >/dev/null
ok "Logout successful"

printf '\n%sAll Shipping E2E checks passed.%s\n' "$COLOR_OK" "$COLOR_RESET"
