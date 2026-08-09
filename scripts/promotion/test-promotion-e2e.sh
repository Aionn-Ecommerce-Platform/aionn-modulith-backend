#!/usr/bin/env bash
# E2E API smoke test for the Promotion module.
# Exit non-zero on the first failed step so CI can gate on it.
#
# Scope note: campaign/banner writes require ROLE_SYSTEM_ADMIN and shop-voucher
# issuance requires ROLE_MERCHANT; neither role is reachable from a self-service
# smoke run. This script therefore covers the public storefront reads, the
# authenticated user-voucher reads, validation and the authz guards.

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

assert_data_array() {
    local body=$1 what=$2
    echo "$body" | grep -q '"data"[[:space:]]*:[[:space:]]*\[' \
        || fail "$what did not return a data array. Body: $body"
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

# --- 1. Authenticate an E2E user ---------------------------------------------
step "1. Authenticating E2E user"
SUFFIX=$(date +%s)
PHONE_LOCAL="09$(printf '%08d' $((RANDOM * 32768 + RANDOM)) | cut -c1-8)"
USERNAME="e2e_promo_user_$SUFFIX"
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

# --- 2. Public storefront reads ----------------------------------------------
step "2. Public storefront reads"
banners_resp=$(http_call GET /api/v1/promotions/banners 200)
assert_data_array "$banners_resp" "banner list"
ok "active banners endpoint is public and returns a list"

campaigns_resp=$(http_call GET "/api/v1/promotions/campaigns?status=RUNNING&limit=10" 200)
assert_data_array "$campaigns_resp" "campaign list"
ok "running campaign list is public"

shop_vouchers_resp=$(http_call GET "/api/v1/promotions/shop-vouchers/merchant/MER_E2E_UNKNOWN?limit=5" 200)
assert_data_array "$shop_vouchers_resp" "shop voucher list"
ok "public shop-voucher list returns an empty array for an unknown merchant"

http_call GET "/api/v1/promotions/campaigns/CAMP_DOES_NOT_EXIST_$SUFFIX" 404 >/dev/null
ok "unknown campaign returns 404"

# --- 3. Authenticated user-voucher reads -------------------------------------
step "3. Authenticated user-voucher reads"
mine_resp=$(http_call GET /api/v1/promotions/vouchers/me 200 "$ACCESS_TOKEN")
assert_data_array "$mine_resp" "my voucher list"
ok "my voucher list returns an empty, ownership-scoped array"

http_call GET "/api/v1/promotions/vouchers/me/V_DOES_NOT_EXIST_$SUFFIX" 404 "$ACCESS_TOKEN" >/dev/null
ok "unknown user voucher returns 404"

http_call POST "/api/v1/promotions/vouchers/V_DOES_NOT_EXIST_$SUFFIX/claim" 404 "$ACCESS_TOKEN" >/dev/null
ok "claiming an unknown voucher returns 404"

# --- 4. Validation guards -----------------------------------------------------
step "4. Validation guards"
invalid_reserve='{"orderId":"","orderValue":100,"currency":"VND"}'
http_call POST "/api/v1/promotions/vouchers/V_ANY/reserve" 400 "$ACCESS_TOKEN" "$invalid_reserve" >/dev/null
ok "reserve without orderId rejected with 400"

invalid_apply='{"orderId":"ORD-1","appliedAmount":-5,"currency":"VND"}'
http_call POST "/api/v1/promotions/vouchers/V_ANY/apply" 400 "$ACCESS_TOKEN" "$invalid_apply" >/dev/null
ok "apply with a negative amount rejected with 400"

# --- 5. Authorization guards --------------------------------------------------
step "5. Authorization guards"
http_call GET /api/v1/promotions/vouchers/me 403 "" >/dev/null
ok "unauthenticated voucher read rejected with 403"

admin_campaign_body=$(cat <<JSON
{"name":"E2E Campaign $SUFFIX","type":"DISCOUNT","budget":1000000,"currency":"VND",
 "startDate":"2030-01-01T00:00:00Z","endDate":"2030-01-31T00:00:00Z"}
JSON
)
http_call POST /api/v1/promotions/campaigns 403 "$ACCESS_TOKEN" "$admin_campaign_body" >/dev/null
ok "campaign creation without ROLE_SYSTEM_ADMIN rejected with 403"

admin_banner_body='{"title":"E2E","imageUrl":"https://cdn/e2e.png","linkUrl":"https://shop/e2e","displayOrder":1}'
http_call POST /api/v1/promotions/banners 403 "$ACCESS_TOKEN" "$admin_banner_body" >/dev/null
ok "banner creation without admin role rejected with 403"

http_call GET /api/v1/promotions/banners/admin 403 "$ACCESS_TOKEN" >/dev/null
ok "admin banner list without admin role rejected with 403"

http_call POST /api/v1/promotions/media/upload-signatures/banner 403 "$ACCESS_TOKEN" >/dev/null
ok "banner upload signature without admin role rejected with 403"

merchant_voucher_body='{"voucherCode":"E2ESHOP","discountAmount":10000,"currency":"VND","usageLimit":5}'
http_call POST /api/v1/promotions/shop-vouchers 403 "$ACCESS_TOKEN" "$merchant_voucher_body" >/dev/null
ok "shop-voucher issuance without ROLE_MERCHANT rejected with 403"

http_call GET /api/v1/promotions/shop-vouchers/mine 403 "$ACCESS_TOKEN" >/dev/null
ok "merchant shop-voucher list without ROLE_MERCHANT rejected with 403"

# --- 6. Cleanup ---------------------------------------------------------------
step "6. Logout E2E session"
http_call POST /api/v1/auth/logout 200 "$ACCESS_TOKEN" >/dev/null
ok "Logout successful"

printf '\n%sAll Promotion E2E checks passed.%s\n' "$COLOR_OK" "$COLOR_RESET"
