#!/usr/bin/env bash
# E2E API smoke test for the Chat module.
# Exit non-zero on the first failed step so CI can gate on it.
#
# Scope note: the STOMP realtime surface and the ROLE_CS_ADMIN support-join
# endpoint are out of reach for a self-service HTTP run, so this script covers
# the authenticated REST surface (conversations, messages, blocks, media
# signature), the validation rules and the authorization guards.

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

register_user() {
    local label=$1
    local phone_local="09$(printf '%08d' $((RANDOM * 32768 + RANDOM)) | cut -c1-8)"
    local username="e2e_chat_${label}_$(date +%s)$RANDOM"

    local initiate_resp reg_id otp_code verify_resp verification_token complete_resp
    initiate_resp=$(http_call POST /api/v1/registrations/initiate 201 "" \
        "{\"phoneNumber\":\"$phone_local\",\"captchaToken\":\"any\"}")
    reg_id=$(json_field "$initiate_resp" "regId")
    otp_code=$(json_field "$initiate_resp" "otpCode")

    verify_resp=$(http_call POST "/api/v1/registrations/$reg_id/verify-otp" 200 "" \
        "{\"otpCode\":\"$otp_code\"}")
    verification_token=$(json_field "$verify_resp" "verificationToken")

    complete_resp=$(http_call POST "/api/v1/registrations/$reg_id/complete" 200 "" \
        "{\"password\":\"E2eTest123!\",\"username\":\"$username\",\"verificationToken\":\"$verification_token\"}")
    json_field "$complete_resp" "accessToken"
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

# --- 1. Authenticate two E2E users -------------------------------------------
step "1. Authenticating E2E users"
SUFFIX=$(date +%s)
BUYER_TOKEN=$(register_user buyer)
[ -n "$BUYER_TOKEN" ] || fail "accessToken missing for buyer"
OUTSIDER_TOKEN=$(register_user outsider)
[ -n "$OUTSIDER_TOKEN" ] || fail "accessToken missing for outsider"
ok "buyer and outsider authenticated"

MERCHANT_ID="MER_E2E_CHAT_$SUFFIX"

# --- 2. Conversation lifecycle ------------------------------------------------
step "2. Conversation lifecycle"
start_body="{\"merchantId\":\"$MERCHANT_ID\",\"buyerDisplayName\":\"E2E Buyer\",\"merchantDisplayName\":\"E2E Shop\"}"
start_resp=$(http_call POST /api/v1/chat/conversations 200 "$BUYER_TOKEN" "$start_body")
CONVERSATION_ID=$(json_field "$start_resp" "conversationId")
[ -n "$CONVERSATION_ID" ] || fail "conversationId missing. Body: $start_resp"
ok "conversation started ($CONVERSATION_ID)"

again_resp=$(http_call POST /api/v1/chat/conversations 200 "$BUYER_TOKEN" "$start_body")
CONVERSATION_ID_AGAIN=$(json_field "$again_resp" "conversationId")
[ "$CONVERSATION_ID" = "$CONVERSATION_ID_AGAIN" ] \
    || fail "starting the same pair created a duplicate ($CONVERSATION_ID vs $CONVERSATION_ID_AGAIN)"
ok "starting the same buyer/merchant pair is idempotent"

http_call POST /api/v1/chat/conversations 400 "$BUYER_TOKEN" '{"merchantId":""}' >/dev/null
ok "blank merchantId rejected with 400"

list_resp=$(http_call GET /api/v1/chat/conversations 200 "$BUYER_TOKEN")
assert_data_array "$list_resp" "conversation list"
echo "$list_resp" | grep -q "$CONVERSATION_ID" \
    || fail "started conversation missing from list. Body: $list_resp"
ok "conversation list is scoped to the caller"

get_resp=$(http_call GET "/api/v1/chat/conversations/$CONVERSATION_ID" 200 "$BUYER_TOKEN")
echo "$get_resp" | grep -q "$MERCHANT_ID" \
    || fail "conversation detail missing merchant. Body: $get_resp"
ok "conversation detail readable by a participant"

http_call GET "/api/v1/chat/conversations/CONV_MISSING_$SUFFIX" 404 "$BUYER_TOKEN" >/dev/null
ok "unknown conversation returns 404"

http_call GET "/api/v1/chat/conversations/$CONVERSATION_ID" 403 "$OUTSIDER_TOKEN" >/dev/null
ok "non-participant read rejected with 403"

unread_resp=$(http_call GET /api/v1/chat/conversations/unread-counts 200 "$BUYER_TOKEN")
echo "$unread_resp" | grep -q '"data"' \
    || fail "unread counts response has no data. Body: $unread_resp"
ok "unread counts returned per conversation"

# --- 3. Messaging -------------------------------------------------------------
step "3. Messaging"
send_body='{"type":"TEXT","body":"Hello from the chat E2E suite"}'
send_resp=$(http_call POST "/api/v1/chat/conversations/$CONVERSATION_ID/messages" 200 \
    "$BUYER_TOKEN" "$send_body")
MESSAGE_ID=$(json_field "$send_resp" "messageId")
[ -n "$MESSAGE_ID" ] || fail "messageId missing. Body: $send_resp"
ok "text message sent ($MESSAGE_ID)"

http_call POST "/api/v1/chat/conversations/$CONVERSATION_ID/messages" 400 \
    "$BUYER_TOKEN" '{"body":"no type"}' >/dev/null
ok "message without a type rejected with 400"

http_call POST "/api/v1/chat/conversations/$CONVERSATION_ID/messages" 400 \
    "$BUYER_TOKEN" '{"type":"TEXT","body":"   "}' >/dev/null
ok "blank text body rejected with 400"

http_call POST "/api/v1/chat/conversations/$CONVERSATION_ID/messages" 400 \
    "$BUYER_TOKEN" '{"type":"IMAGE","metadata":{}}' >/dev/null
ok "IMAGE message without imageUrl rejected with 400"

http_call POST "/api/v1/chat/conversations/$CONVERSATION_ID/messages" 403 \
    "$OUTSIDER_TOKEN" "$send_body" >/dev/null
ok "non-participant send rejected with 403"

messages_resp=$(http_call GET "/api/v1/chat/conversations/$CONVERSATION_ID/messages" 200 "$BUYER_TOKEN")
assert_data_array "$messages_resp" "message list"
echo "$messages_resp" | grep -q "$MESSAGE_ID" \
    || fail "sent message missing from history. Body: $messages_resp"
ok "message history contains the sent message"

http_call POST "/api/v1/chat/conversations/$CONVERSATION_ID/typing" 200 \
    "$BUYER_TOKEN" '{"typing":true}' >/dev/null
ok "typing state accepted"

http_call POST "/api/v1/chat/messages/$MESSAGE_ID/delivered" 200 "$BUYER_TOKEN" >/dev/null
ok "delivery acknowledgement accepted"

http_call POST "/api/v1/chat/messages/MSG_MISSING_$SUFFIX/read" 404 "$BUYER_TOKEN" >/dev/null
ok "acknowledging an unknown message returns 404"

read_resp=$(http_call POST "/api/v1/chat/conversations/$CONVERSATION_ID/read" 200 "$BUYER_TOKEN")
echo "$read_resp" | grep -q "$CONVERSATION_ID" \
    || fail "mark-read response missing the conversation. Body: $read_resp"
ok "conversation marked read"

recall_resp=$(http_call POST "/api/v1/chat/messages/$MESSAGE_ID/recall" 200 "$BUYER_TOKEN")
echo "$recall_resp" | grep -q 'RECALLED' \
    || fail "recalled message is not in RECALLED state. Body: $recall_resp"
ok "message recalled inside the recall window"

http_call POST "/api/v1/chat/messages/$MESSAGE_ID/recall" 400 "$BUYER_TOKEN" >/dev/null
ok "recalling twice rejected with 400"

# --- 4. Archive ---------------------------------------------------------------
step "4. Archive / unarchive"
http_call POST "/api/v1/chat/conversations/$CONVERSATION_ID/archive" 200 "$BUYER_TOKEN" >/dev/null
ok "conversation archived"

hidden_resp=$(http_call GET /api/v1/chat/conversations 200 "$BUYER_TOKEN")
echo "$hidden_resp" | grep -q "$CONVERSATION_ID" \
    && fail "archived conversation still listed by default. Body: $hidden_resp"
ok "archived conversation hidden from the default list"

included_resp=$(http_call GET "/api/v1/chat/conversations?includeArchived=true" 200 "$BUYER_TOKEN")
echo "$included_resp" | grep -q "$CONVERSATION_ID" \
    || fail "archived conversation missing with includeArchived=true. Body: $included_resp"
ok "archived conversation visible with includeArchived=true"

http_call POST "/api/v1/chat/conversations/$CONVERSATION_ID/unarchive" 200 "$BUYER_TOKEN" >/dev/null
ok "conversation unarchived"

# --- 5. Blocks ----------------------------------------------------------------
step "5. User blocks"
BLOCKED_ID="U_E2E_BLOCKED_$SUFFIX"
block_resp=$(http_call POST /api/v1/chat/blocks 200 "$BUYER_TOKEN" \
    "{\"blockedId\":\"$BLOCKED_ID\",\"reason\":\"spam\"}")
BLOCK_ID=$(json_field "$block_resp" "blockId")
[ -n "$BLOCK_ID" ] || fail "blockId missing. Body: $block_resp"
ok "user blocked ($BLOCK_ID)"

blocks_resp=$(http_call GET /api/v1/chat/blocks 200 "$BUYER_TOKEN")
assert_data_array "$blocks_resp" "block list"
echo "$blocks_resp" | grep -q "$BLOCKED_ID" \
    || fail "blocked user missing from list. Body: $blocks_resp"
ok "block list contains the blocked user"

http_call POST /api/v1/chat/blocks 400 "$BUYER_TOKEN" '{"blockedId":""}' >/dev/null
ok "blank blockedId rejected with 400"

http_call DELETE "/api/v1/chat/blocks/$BLOCKED_ID" 200 "$BUYER_TOKEN" >/dev/null
ok "user unblocked"

http_call DELETE "/api/v1/chat/blocks/$BLOCKED_ID" 404 "$BUYER_TOKEN" >/dev/null
ok "unblocking an inactive block returns 404"

# --- 6. Media upload signature ------------------------------------------------
step "6. Chat media upload signature"
signature_resp=$(http_call POST /api/v1/chat/media/upload-signatures/image 200 "$BUYER_TOKEN")
echo "$signature_resp" | grep -q '"signature"' \
    || fail "upload signature response has no signature. Body: $signature_resp"
echo "$signature_resp" | grep -q 'aionn/chat/images/' \
    || fail "upload signature folder is not scoped to chat images. Body: $signature_resp"
ok "signed upload params scoped to the caller"

# --- 7. Authorization guards --------------------------------------------------
step "7. Authorization guards"
http_call GET /api/v1/chat/conversations 403 "" >/dev/null
ok "unauthenticated conversation list rejected with 403"

http_call POST /api/v1/chat/media/upload-signatures/image 403 "" >/dev/null
ok "unauthenticated upload signature rejected with 403"

http_call GET "/api/v1/chat/merchants/$MERCHANT_ID/auto-reply" 403 "$BUYER_TOKEN" >/dev/null
ok "auto-reply read by a non-owner rejected with 403"

auto_reply_body='{"enabled":true,"awayMessage":"Away","workingHourStart":"08:00","workingHourEnd":"22:00"}'
http_call PUT "/api/v1/chat/merchants/$MERCHANT_ID/auto-reply" 403 "$BUYER_TOKEN" "$auto_reply_body" >/dev/null
ok "auto-reply update by a non-owner rejected with 403"

http_call POST "/api/v1/chat/conversations/$CONVERSATION_ID/support" 403 "$BUYER_TOKEN" >/dev/null
ok "support join without ROLE_CS_ADMIN rejected with 403"

# --- 8. Cleanup ---------------------------------------------------------------
step "8. Logout E2E sessions"
http_call POST /api/v1/auth/logout 200 "$BUYER_TOKEN" >/dev/null
http_call POST /api/v1/auth/logout 200 "$OUTSIDER_TOKEN" >/dev/null
ok "Logout successful"

printf '\n%sAll Chat E2E checks passed.%s\n' "$COLOR_OK" "$COLOR_RESET"
