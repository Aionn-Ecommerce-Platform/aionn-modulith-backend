#!/usr/bin/env bash
# E2E API smoke test for the Recommendation module.
# Exit non-zero on the first failed step so CI can gate on it.
#
# Run through scripts/e2e/run-e2e-suite.ps1, which starts the application against an isolated database,
# applies scripts/e2e/e2e-prerequisites.sql (this script needs the CAT_E2E category it creates)
# and shortens the recommendation offline-job cadence so step 6 can observe a profile refresh inside
# one run. Running it standalone against a default-configured application will work for steps 1-5 and
# 7-8, but step 6 waits on the profile refresh scheduler and will time out at the production cadence.

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

status_call() {
    local response
    response=$(curl --noproxy "*" -sS -o - -w '\n<<<STATUS>>>%{http_code}' "$@") || return 1
    printf '%s' "${response##*<<<STATUS>>>}"
}

http_call() {
    local method=$1 path=$2 expected=$3 bearer=${4:-} body=${5:-}
    local url="$BASE_URL$path"
    local hdr=(-H 'Accept: application/json')
    [ -n "$body" ]   && hdr+=(-H 'Content-Type: application/json')
    [ -n "$bearer" ] && hdr+=(-H "Authorization: Bearer $bearer")
    [ -n "${MERCHANT_ID:-}" ] && hdr+=(-H "X-Merchant-Id: $MERCHANT_ID")
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

# A recommendation slate is a flat array of objects, so one item can be isolated without a JSON parser.
slate_item() {
    local body=$1 product_id=$2
    printf '%s' "$body" \
        | grep -oE '\{[^{}]*"productId"[[:space:]]*:[[:space:]]*"'"$product_id"'"[^{}]*\}' \
        | head -1
}

item_reason() {
    json_field "$(slate_item "$1" "$2")" "reason"
}

# Polls until the given command succeeds, so a step that depends on an offline job does not have to
# guess how long the job takes. Fails the run with the last response rather than hanging.
poll_until() {
    local description=$1 timeout_seconds=$2 check=$3
    local deadline=$(( $(date +%s) + timeout_seconds ))
    while [ "$(date +%s)" -lt "$deadline" ]; do
        if eval "$check"; then
            ok "$description"
            return 0
        fi
        sleep 3
    done
    fail "$description did not happen within ${timeout_seconds}s"
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

# --- 1. Identity and merchant -------------------------------------------------
step "1. Authenticating E2E user and registering a merchant"
SUFFIX=$(date +%s)
PHONE_LOCAL="09$(printf '%08d' $((RANDOM * 32768 + RANDOM)) | cut -c1-8)"
USERNAME="e2e_rec_user_$SUFFIX"
PASSWORD='E2eTest123!'

initiate_body="{\"phoneNumber\":\"$PHONE_LOCAL\",\"captchaToken\":\"any\"}"
initiate_resp=$(http_call POST /api/v1/registrations/initiate 201 "" "$initiate_body")
REG_ID=$(json_field "$initiate_resp" "regId")
OTP_CODE=$(json_field "$initiate_resp" "otpCode")

verify_resp=$(http_call POST "/api/v1/registrations/$REG_ID/verify-otp" 200 "" \
    "{\"otpCode\":\"$OTP_CODE\"}")
VERIFICATION_TOKEN=$(json_field "$verify_resp" "verificationToken")

complete_body="{\"password\":\"$PASSWORD\",\"username\":\"$USERNAME\",\"verificationToken\":\"$VERIFICATION_TOKEN\"}"
complete_resp=$(http_call POST "/api/v1/registrations/$REG_ID/complete" 200 "" "$complete_body")
ACCESS_TOKEN=$(json_field "$complete_resp" "accessToken")
[ -n "$ACCESS_TOKEN" ] || fail "no access token returned"

merchant_resp=$(http_call POST /api/v1/catalog/merchants 201 "$ACCESS_TOKEN" \
    "{\"name\":\"E2E Rec Merchant $SUFFIX\"}")
MERCHANT_ID=$(json_field "$merchant_resp" "merchantId")
[ -n "$MERCHANT_ID" ] || fail "no merchantId returned"
ok "merchant registered with id=$MERCHANT_ID"

warehouse_resp=$(http_call POST /api/v1/inventory/warehouses 201 "$ACCESS_TOKEN" \
    '{"address":"123 Modulith Street, Zone E2E","priorityLevel":1}')
WH_ID=$(json_field "$warehouse_resp" "warehouseId")
[ -n "$WH_ID" ] || fail "no warehouseId returned"

# --- 2. Two published, in-stock products in different categories --------------
step "2. Creating two published products with stock"
SKU_A="SKU-REC-A-$SUFFIX"
SKU_B="SKU-REC-B-$SUFFIX"

create_and_publish() {
    local name=$1 sku=$2
    local product_id
    product_id=$(json_field "$(http_call POST /api/v1/catalog/products 201 "$ACCESS_TOKEN" \
        "{\"name\":\"$name\"}")" "productId")
    [ -n "$product_id" ] || fail "no productId returned for $name"

    # attributeValues must be present, not merely optional: the catalog service calls Map.isEmpty() on
    # it unconditionally, so an omitted field comes back as a 500 rather than a 400.
    http_call POST "/api/v1/catalog/products/$product_id/variants" 200 "$ACCESS_TOKEN" \
        "{\"skuId\":\"$sku\",\"attributeValues\":{},\"price\":34990000,\"currency\":\"VND\"}" >/dev/null
    # Product.publish() refuses a product with no category, and creating one needs ROLE_SYSTEM_ADMIN,
    # so both products share the fixture's CAT_E2E. That is enough for step 6: what has to change is
    # whether the user has a profile at all, not which category each product sits in.
    http_call PUT "/api/v1/catalog/products/$product_id/categories" 200 "$ACCESS_TOKEN" \
        '{"categoryIds":["CAT_E2E"]}' >/dev/null
    http_call POST "/api/v1/catalog/products/$product_id/publish" 200 "$ACCESS_TOKEN" >/dev/null

    http_call POST /api/v1/inventory/items 201 "$ACCESS_TOKEN" \
        "{\"skuId\":\"$sku\",\"warehouseId\":\"$WH_ID\",\"initialQty\":50}" >/dev/null
    printf '%s' "$product_id"
}

PRODUCT_A=$(create_and_publish "E2E Rec Phone $SUFFIX" "$SKU_A")
PRODUCT_B=$(create_and_publish "E2E Rec Cable $SUFFIX" "$SKU_B")
ok "published $PRODUCT_A and $PRODUCT_B, both in stock"

# --- 3. Public surfaces answer without a token --------------------------------
step "3. Public recommendation surfaces are reachable anonymously"
anon_home=$(http_call GET '/api/v1/recommendations/home?limit=10' 200)
[ -n "$(printf '%s' "$anon_home" | grep -o '"data"')" ] || fail "home feed returned no data envelope"
ok "anonymous home feed returned 200"

http_call GET "/api/v1/recommendations/products/$PRODUCT_A/similar?limit=5" 200 >/dev/null
ok "similar products returned 200"

http_call GET "/api/v1/recommendations/products/$PRODUCT_A/also-bought?limit=5" 200 >/dev/null
ok "also-bought returned 200"

# An unknown product must fall back to trending rather than 404 or 500: the interaction log keeps IDs
# catalog may have retired, and a product page linking a deleted item is a normal state.
http_call GET "/api/v1/recommendations/products/01HZUNKNOWN0000000000000/similar?limit=5" 200 >/dev/null
ok "unknown product falls back to trending with 200"

# --- 4. Authorization boundary -------------------------------------------------
step "4. Cart suggestions require authentication"
# Deliberately bypasses http_call, which asserts success: this call is expected to be refused.
cart_status=$(status_call \
    -H 'Accept: application/json' -H 'X-Client-Type: mobile' \
    "$BASE_URL/api/v1/recommendations/cart/suggestions?skuIds=$SKU_A")
[ "$cart_status" = "401" ] || [ "$cart_status" = "403" ] \
    || fail "unauthenticated cart suggestions expected 401/403 got $cart_status"
ok "unauthenticated cart suggestions refused with $cart_status"

http_call GET "/api/v1/recommendations/cart/suggestions?skuIds=$SKU_A&limit=5" 200 "$ACCESS_TOKEN" >/dev/null
ok "authenticated cart suggestions returned 200"

# --- 5. Limit bounds are enforced at the boundary ------------------------------
step "5. Page-size bounds are rejected rather than passed downstream"
# An unclamped limit reaches new ArrayList<>(capacity) inside the read service and comes back as a 500
# on an anonymous request, so these are checked against the real HTTP surface and not only in unit tests.
for bad_limit in 0 -1 51 2147483647; do
        bad_status=$(status_call \
        -H 'Accept: application/json' \
        "$BASE_URL/api/v1/recommendations/home?limit=$bad_limit")
    [ "$bad_status" = "400" ] || fail "limit=$bad_limit expected 400 got $bad_status"
done
ok "limit 0, -1, 51 and 2147483647 all rejected with 400"

empty_cart_status=$(status_call \
    -H 'Accept: application/json' -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "X-Merchant-Id: $MERCHANT_ID" -H 'X-Client-Type: mobile' \
    "$BASE_URL/api/v1/recommendations/cart/suggestions")
[ "$empty_cart_status" = "400" ] || fail "cart suggestions with no skuIds expected 400 got $empty_cart_status"
ok "cart suggestions with an empty basket rejected with 400"

# --- 6. Behavioural signal reaches the personalised feed -----------------------
step "6. Views flow through the outbox into a personalised home feed"
# No interaction-write endpoint exists by design: the only way to create a signal is to perform the
# business action that owns it. This is the chain that has to work end to end -
# catalog view -> ProductViewedIntegrationEvent -> outbox -> ingest listener -> interaction row ->
# profile refresh scheduler -> affinity -> personalised ranking.
for _ in 1 2 3; do
    http_call POST "/api/v1/catalog/products/$PRODUCT_A/view" 200 "$ACCESS_TOKEN" >/dev/null
done
ok "three product views recorded against $PRODUCT_A"

# Content scores are computed from the stored affinity profile, so until a refresh has run the user has
# no content signal at all and A can only be explained by momentum. MATCHES_YOUR_INTERESTS is therefore
# evidence that the whole chain ran, not just that the product is in the feed.
affinity_explains_product_a() {
    local feed
    feed=$(http_call GET '/api/v1/recommendations/home?limit=10' 200 "$ACCESS_TOKEN")
    [ "$(item_reason "$feed" "$PRODUCT_A")" = 'MATCHES_YOUR_INTERESTS' ]
}
poll_until "personalised home feed explains $PRODUCT_A by content affinity" 180 \
    affinity_explains_product_a

# --- 7. Availability is filtered outside the cache -----------------------------
step "7. Sold-out products leave the feed without waiting for the cache to expire"
# The slate is cached for minutes but availability is re-checked per request, by design: reversing that
# ordering would serve out-of-stock products for the whole TTL. Auditing the stock to zero is the real
# operational action, not a cache flush.
http_call POST "/api/v1/inventory/items/$SKU_A/$WH_ID/audit" 200 "$ACCESS_TOKEN" '{"actualQty":0}' >/dev/null
ok "stock for $SKU_A audited to zero"

# B stays in stock, so it must survive. Checking only that A vanished would also pass if the filter had
# thrown the whole slate away.
feed_drops_a_and_keeps_b() {
    local feed
    feed=$(http_call GET '/api/v1/recommendations/home?limit=10' 200 "$ACCESS_TOKEN")
    ! printf '%s' "$feed" | grep -q "$PRODUCT_A" \
        && printf '%s' "$feed" | grep -q "$PRODUCT_B"
}
poll_until "$PRODUCT_A drops out of the home feed while $PRODUCT_B stays" 90 \
    feed_drops_a_and_keeps_b

# --- 8. Cleanup ----------------------------------------------------------------
step "8. Logout E2E session"
http_call POST /api/v1/auth/logout 200 "$ACCESS_TOKEN" >/dev/null
ok "Logout successful"

printf '\n%sAll Recommendation E2E checks passed.%s\n' "$COLOR_OK" "$COLOR_RESET"
