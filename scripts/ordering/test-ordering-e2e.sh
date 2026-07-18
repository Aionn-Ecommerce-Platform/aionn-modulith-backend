#!/usr/bin/env bash
# E2E API smoke test for the Ordering module.
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
    if [ -n "$bearer" ]; then
        # For merchant ownership checks, we also append X-Merchant-Id header if we have one
        if [[ -n "${MERCHANT_ID:-}" ]]; then
            hdr+=(-H "X-Merchant-Id: $MERCHANT_ID")
        fi
    fi

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

# --- 1. Acquire access token for customer -------------------------------------
step "1. Register a temporary user for authentication"
SUFFIX=$(date +%s)
PHONE_LOCAL="09$(printf '%08d' $((RANDOM * 32768 + RANDOM)) | cut -c1-8)"
USERNAME="e2e_user_$SUFFIX"
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
[ -n "$ACCESS_TOKEN" ] || fail "accessToken missing"
ok "temporary user authentication acquired"

# --- 2. Register Merchant & Create Product to E2E order on --------------------
step "2. Register merchant & create catalog product variants"
merchant_body="{\"name\":\"E2E Merchant $SUFFIX\"}"
merchant_resp=$(http_call POST "/api/v1/catalog/merchants" 201 "$ACCESS_TOKEN" "$merchant_body")
MERCHANT_ID=$(json_field "$merchant_resp" "merchantId")
[ -n "$MERCHANT_ID" ] || fail "merchantId missing"
ok "merchant registered id=$MERCHANT_ID"

# 2.1 Create warehouse for the merchant
warehouse_body="{\"address\":\"E2E Warehouse Address $SUFFIX\",\"priorityLevel\":1}"
warehouse_resp=$(http_call POST "/api/v1/inventory/warehouses" 201 "$ACCESS_TOKEN" "$warehouse_body")
WAREHOUSE_ID=$(json_field "$warehouse_resp" "warehouseId")
[ -n "$WAREHOUSE_ID" ] || fail "warehouseId missing"
ok "warehouse created id=$WAREHOUSE_ID"

product_body="{\"name\":\"E2E Product $SUFFIX\"}"
product_resp=$(http_call POST "/api/v1/catalog/products" 201 "$ACCESS_TOKEN" "$product_body")
PRODUCT_ID=$(json_field "$product_resp" "productId")
[ -n "$PRODUCT_ID" ] || fail "productId missing"
ok "product created id=$PRODUCT_ID"

DYNAMIC_SKU="SKU_E2E_$SUFFIX"
variant_body="{\"skuId\":\"$DYNAMIC_SKU\",\"attributeValues\":{},\"price\":150000.0,\"currency\":\"VND\"}"
http_call POST "/api/v1/catalog/products/$PRODUCT_ID/variants" 200 "$ACCESS_TOKEN" "$variant_body" >/dev/null
ok "variant defined with sku=$DYNAMIC_SKU"

category_body="{\"categoryIds\":[\"CAT_SMH\"]}"
http_call PUT "/api/v1/catalog/products/$PRODUCT_ID/categories" 200 "$ACCESS_TOKEN" "$category_body" >/dev/null
ok "category assigned to product"

http_call POST "/api/v1/catalog/products/$PRODUCT_ID/publish" 200 "$ACCESS_TOKEN" "" >/dev/null
ok "product published successfully"

# 2.2 Initialize stock for the variant in our warehouse
stock_body="{\"skuId\":\"$DYNAMIC_SKU\",\"warehouseId\":\"$WAREHOUSE_ID\",\"initialQty\":100}"
http_call POST "/api/v1/inventory/items" 201 "$ACCESS_TOKEN" "$stock_body" >/dev/null
ok "stock initialized with 100 items"

# --- 3. Ordering list empty orders ---------------------------------------------
step "3. List orders (should be empty)"
list_resp=$(http_call GET "/api/v1/ordering/orders" 200 "$ACCESS_TOKEN")
ok "GET /api/v1/ordering/orders returned 200"

# --- 4. Add item to cart ------------------------------------------------------
step "4. Add item to cart"
cart_body="{\"skuId\":\"$DYNAMIC_SKU\",\"qty\":1}"
cart_resp=$(http_call POST "/api/v1/ordering/cart/items" 200 "$ACCESS_TOKEN" "$cart_body")
ok "POST /api/v1/ordering/cart/items returned 200"

# --- 5. Place order ------------------------------------------------------------
step "5. Place an order"
order_body="{\"addressId\":\"addr-1\",\"gateway\":\"COD\",\"currency\":\"VND\",\"shippingFee\":30000,\"shippingAddress\":{\"fullName\":\"Nguyen E2E\",\"phone\":\"$PHONE_LOCAL\",\"province\":\"HN\",\"district\":\"CG\",\"ward\":\"DV\",\"street\":\"DT\"},\"selectedSkuIds\":[\"$DYNAMIC_SKU\"]}"
order_resp=$(http_call POST "/api/v1/ordering/orders" 201 "$ACCESS_TOKEN" "$order_body")
ORDER_ID=$(json_field "$order_resp" "orderId")
[ -n "$ORDER_ID" ] || fail "orderId missing"
ok "order placed successfully id=$ORDER_ID"

# --- 6. Get order details ------------------------------------------------------
step "6. Get order details"
http_call GET "/api/v1/ordering/orders/$ORDER_ID" 200 "$ACCESS_TOKEN" >/dev/null
ok "GET /api/v1/ordering/orders/$ORDER_ID returned 200"

# --- 7. User cancel order ------------------------------------------------------
step "7. Cancel the order"
cancel_body="{\"reason\":\"changed_mind\"}"
http_call POST "/api/v1/ordering/orders/$ORDER_ID/cancel" 200 "$ACCESS_TOKEN" "$cancel_body" >/dev/null
ok "POST /api/v1/ordering/orders/$ORDER_ID/cancel returned 200"

# --- 8. Cleanup ---------------------------------------------------------------
step "8. Cleanup"
http_call POST /api/v1/auth/logout 200 "$ACCESS_TOKEN" >/dev/null
ok "logout returned 200"

printf '\n%sAll Ordering E2E checks passed.%s\n' "$COLOR_OK" "$COLOR_RESET"
