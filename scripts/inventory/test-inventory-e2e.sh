#!/usr/bin/env bash
# E2E API smoke test for the Inventory module.
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

json_field_num() {
    local body=$1 key=$2
    printf '%s' "$body" \
        | grep -oE "\"$key\"[[:space:]]*:[[:space:]]*[0-9]+" \
        | head -1 \
        | sed -E "s/.*\"$key\"[[:space:]]*:[[:space:]]*([0-9]+).*/\1/"
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

# --- 1. User Registration & Merchant Registration -----------------------------
step "1. Authenticating E2E User and Registering Merchant"
SUFFIX=$(date +%s)
PHONE_LOCAL="09$(printf '%08d' $((RANDOM * 32768 + RANDOM)) | cut -c1-8)"
USERNAME="e2e_inv_user_$SUFFIX"
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

# Register Merchant to be allowed to manage warehouses/inventory
merchant_body="{\"name\":\"E2E Inv Merchant $SUFFIX\"}"
merchant_resp=$(http_call POST /api/v1/catalog/merchants 201 "$ACCESS_TOKEN" "$merchant_body")
MERCHANT_ID=$(json_field "$merchant_resp" "merchantId")
ok "Merchant registered with id=$MERCHANT_ID"

# --- 2. Warehouse Operations -------------------------------------------------
step "2. Warehouse CRUD Operations"
wh_create_body="{\"address\":\"123 Modulith Street, Zone E2E\",\"priorityLevel\":1}"
wh_resp=$(http_call POST /api/v1/inventory/warehouses 201 "$ACCESS_TOKEN" "$wh_create_body")
WH_ID=$(json_field "$wh_resp" "warehouseId")
[ -n "$WH_ID" ] || fail "warehouseId missing from response"
ok "Warehouse created with id=$WH_ID"

# Update Priority
wh_priority_body="{\"priorityLevel\":5}"
http_call PUT "/api/v1/inventory/warehouses/$WH_ID/priority" 200 "$ACCESS_TOKEN" "$wh_priority_body" >/dev/null
ok "Warehouse priority adjusted"

# Get Warehouses owned by Merchant
wh_list_resp=$(http_call GET /api/v1/inventory/warehouses 200 "$ACCESS_TOKEN")
ok "Active warehouses listed successfully"

# --- 3. Inventory Item Operations --------------------------------------------
step "3. Inventory Item Stock Management"
SKU_ID="SKU-E2E-$SUFFIX"

# Initialize Stock
init_stock_body="{\"skuId\":\"$SKU_ID\",\"warehouseId\":\"$WH_ID\",\"initialQty\":100}"
init_resp=$(http_call POST /api/v1/inventory/items 201 "$ACCESS_TOKEN" "$init_stock_body")
ok "Inventory stock initialized for SKU=$SKU_ID"

# Get Stock detail
item_get_resp=$(http_call GET "/api/v1/inventory/items/$SKU_ID/$WH_ID" 200 "$ACCESS_TOKEN")
PHYSICAL_QTY=$(json_field_num "$item_get_resp" "physicalQty")
[ "$PHYSICAL_QTY" = "100" ] || fail "Physical quantity mismatch: expected 100, got $PHYSICAL_QTY"
ok "Physical stock verified"

# Configure Safety Stock
safety_body="{\"safetyStockQty\":15}"
safety_resp=$(http_call PUT "/api/v1/inventory/items/$SKU_ID/$WH_ID/safety-stock" 200 "$ACCESS_TOKEN" "$safety_body")
SAFETY_QTY=$(json_field_num "$safety_resp" "safetyStockQty")
[ "$SAFETY_QTY" = "15" ] || fail "Safety stock qty mismatch"
ok "Safety stock configured to 15"

# Record Manual Adjustment (Increase)
adj_body="{\"qty\":20,\"type\":\"MANUAL_INCREASE\",\"reason\":\"E2E manual restock\"}"
adj_resp=$(http_call POST "/api/v1/inventory/items/$SKU_ID/$WH_ID/manual-adjustment" 200 "$ACCESS_TOKEN" "$adj_body")
NEW_PHYSICAL=$(json_field_num "$adj_resp" "physicalQty")
[ "$NEW_PHYSICAL" = "120" ] || fail "Manual adjustment failed: expected 120, got $NEW_PHYSICAL"
ok "Manual adjustment (Increase) successful"

# Record Audit Reconcile
audit_body="{\"actualQty\":110}"
audit_resp=$(http_call POST "/api/v1/inventory/items/$SKU_ID/$WH_ID/audit" 200 "$ACCESS_TOKEN" "$audit_body")
AUDITED_QTY=$(json_field_num "$audit_resp" "physicalQty")
[ "$AUDITED_QTY" = "110" ] || fail "Audit mismatch"
ok "Audit quantity reconciled to 110"

# --- 4. Cleanup ---------------------------------------------------------------
step "4. Logout E2E session"
http_call POST /api/v1/auth/logout 200 "$ACCESS_TOKEN" >/dev/null
ok "Logout successful"

printf '\n%sAll Inventory E2E checks passed.%s\n' "$COLOR_OK" "$COLOR_RESET"
