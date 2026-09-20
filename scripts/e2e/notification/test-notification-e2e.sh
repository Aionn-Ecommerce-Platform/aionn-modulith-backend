#!/usr/bin/env bash
# E2E API smoke test for the Notification module.
# Exit non-zero on the first failed step so CI can gate on it.
#
# Scope note: dispatch, template and provider endpoints require an admin role
# that a self-service run cannot obtain, so this script covers the authenticated
# per-user surface (subscriptions, device tokens, inbox), validation and the
# authz guards. Registration OTP delivery exercises the module indirectly: the
# identity flow calls IdentityNotificationPort, which is now backed by the real
# notification adapter instead of the stub.

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
step "1. Authenticating E2E user (also exercises registration-OTP notification)"
SUFFIX=$(date +%s)
PHONE_LOCAL="09$(printf '%08d' $((RANDOM * 32768 + RANDOM)) | cut -c1-8)"
USERNAME="e2e_notif_user_$SUFFIX"
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

# --- 2. Subscription defaults -------------------------------------------------
step "2. Subscription defaults"
sub_resp=$(http_call GET /api/v1/notifications/subscriptions/me 200 "$ACCESS_TOKEN")
echo "$sub_resp" | grep -q '"settings"' \
    || fail "subscription response has no settings map. Body: $sub_resp"
ok "default subscription is materialised on first read"

opt_out_body='{"category":"PROMOTION","channel":"EMAIL","enabled":false}'
opt_out_resp=$(http_call PUT /api/v1/notifications/subscriptions/me 200 "$ACCESS_TOKEN" "$opt_out_body")
echo "$opt_out_resp" | grep -q '"PROMOTION"' \
    || fail "updated subscription does not contain PROMOTION. Body: $opt_out_resp"
ok "opting out of a promotional channel persists"

mandatory_body='{"category":"SECURITY","channel":"EMAIL","enabled":false}'
http_call PUT /api/v1/notifications/subscriptions/me 400 "$ACCESS_TOKEN" "$mandatory_body" >/dev/null
ok "disabling a mandatory SECURITY channel is rejected with 400"

invalid_body='{"channel":"EMAIL","enabled":false}'
http_call PUT /api/v1/notifications/subscriptions/me 400 "$ACCESS_TOKEN" "$invalid_body" >/dev/null
ok "subscription update without category rejected with 400"

# --- 3. Device tokens ---------------------------------------------------------
step "3. Device token lifecycle"
DEVICE_TOKEN="fcm-e2e-$SUFFIX"
register_body="{\"deviceToken\":\"$DEVICE_TOKEN\",\"os\":\"android\"}"
register_resp=$(http_call POST /api/v1/notifications/subscriptions/me/device-tokens 201 \
    "$ACCESS_TOKEN" "$register_body")
TOKEN_ID=$(json_field "$register_resp" "tokenId")
[ -n "$TOKEN_ID" ] || fail "tokenId missing from device-token response"
ok "device token registered ($TOKEN_ID)"

again_resp=$(http_call POST /api/v1/notifications/subscriptions/me/device-tokens 201 \
    "$ACCESS_TOKEN" "$register_body")
TOKEN_ID_AGAIN=$(json_field "$again_resp" "tokenId")
[ "$TOKEN_ID" = "$TOKEN_ID_AGAIN" ] \
    || fail "re-registering the same token created a duplicate ($TOKEN_ID vs $TOKEN_ID_AGAIN)"
ok "re-registering the same token is idempotent"

tokens_resp=$(http_call GET /api/v1/notifications/subscriptions/me/device-tokens 200 "$ACCESS_TOKEN")
assert_data_array "$tokens_resp" "device token list"
echo "$tokens_resp" | grep -q "$DEVICE_TOKEN" \
    || fail "registered token missing from list. Body: $tokens_resp"
ok "device token list contains the registered token"

blank_token_body='{"deviceToken":"","os":"android"}'
http_call POST /api/v1/notifications/subscriptions/me/device-tokens 400 \
    "$ACCESS_TOKEN" "$blank_token_body" >/dev/null
ok "blank device token rejected with 400"

http_call DELETE "/api/v1/notifications/subscriptions/me/device-tokens/$TOKEN_ID" 204 \
    "$ACCESS_TOKEN" >/dev/null
ok "device token removed with 204"

http_call DELETE "/api/v1/notifications/subscriptions/me/device-tokens/TOK_MISSING_$SUFFIX" 404 \
    "$ACCESS_TOKEN" >/dev/null
ok "removing an unknown device token returns 404"

# --- 4. Inbox -----------------------------------------------------------------
step "4. Notification inbox"
inbox_resp=$(http_call GET /api/v1/notifications 200 "$ACCESS_TOKEN")
assert_data_array "$inbox_resp" "notification inbox"
ok "inbox returns an ownership-scoped list"

http_call GET "/api/v1/notifications/NOTI_MISSING_$SUFFIX" 404 "$ACCESS_TOKEN" >/dev/null
ok "unknown notification returns 404"

http_call POST "/api/v1/notifications/NOTI_MISSING_$SUFFIX/read" 404 "$ACCESS_TOKEN" >/dev/null
ok "marking an unknown notification read returns 404"

http_call DELETE "/api/v1/notifications/NOTI_MISSING_$SUFFIX" 404 "$ACCESS_TOKEN" >/dev/null
ok "deleting an unknown notification returns 404"

# --- 5. Authorization guards --------------------------------------------------
step "5. Authorization guards"
http_call GET /api/v1/notifications 403 "" >/dev/null
ok "unauthenticated inbox read rejected with 403"

dispatch_body=$(cat <<JSON
{"userId":"user-e2e","eventType":"identity.password-changed","category":"SECURITY",
 "channels":["EMAIL"],"context":{"channelHint":"email"}}
JSON
)
http_call POST /api/v1/notifications/dispatch 403 "$ACCESS_TOKEN" "$dispatch_body" >/dev/null
ok "dispatch without admin role rejected with 403"

template_body=$(cat <<JSON
{"eventType":"e2e.event","channel":"EMAIL","category":"SYSTEM","locale":"vi-VN",
 "subject":"E2E","content":"Hello {{name}}"}
JSON
)
http_call POST /api/v1/notifications/templates 403 "$ACCESS_TOKEN" "$template_body" >/dev/null
ok "template creation without admin role rejected with 403"

http_call GET /api/v1/notifications/templates 403 "$ACCESS_TOKEN" >/dev/null
ok "template list without admin role rejected with 403"

provider_body='{"channel":"EMAIL","providerType":"smtp","config":{"host":"localhost"},"rateLimitPerMinute":60}'
http_call POST /api/v1/notifications/providers 403 "$ACCESS_TOKEN" "$provider_body" >/dev/null
ok "provider configuration without admin role rejected with 403"

http_call GET /api/v1/notifications/providers 403 "$ACCESS_TOKEN" >/dev/null
ok "provider list without admin role rejected with 403"

http_call GET "/api/v1/notifications/analytics?campaignId=camp-e2e" 403 "$ACCESS_TOKEN" >/dev/null
ok "campaign analytics without admin role rejected with 403"

# --- 6. Cleanup ---------------------------------------------------------------
step "6. Logout E2E session"
http_call POST /api/v1/auth/logout 200 "$ACCESS_TOKEN" >/dev/null
ok "Logout successful"

printf '\n%sAll Notification E2E checks passed.%s\n' "$COLOR_OK" "$COLOR_RESET"
