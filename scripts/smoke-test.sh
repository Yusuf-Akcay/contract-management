#!/usr/bin/env bash
# Smoke test against a running stack (docker compose --profile app up).
# Needs curl + jq. Override hosts for CI runners: API_URL=http://docker:8080 KEYCLOAK_URL=http://docker:8180
# SMOKE_RAG=1 additionally checks upload + question answering (needs Ollama, e.g. --profile rag).
set -u
API=${API_URL:-http://localhost:8080}
KC=${KEYCLOAK_URL:-http://localhost:8180}/realms/contract-management/protocol/openid-connect/token
fail=0

check() { if [ "$2" = "$3" ]; then echo "OK   $1"; else echo "FAIL $1: expected $2, got $3"; fail=1; fi; }
status() { curl -s -o /dev/null -w '%{http_code}' "$@"; }
token() { curl -s -X POST "$KC" -d grant_type=password -d client_id=contract-management -d username="$1" -d password=demo | jq -r .access_token; }
wait_for() {
  for _ in $(seq 1 100); do [ "$(status "$2")" = 200 ] && return; sleep 3; done
  echo "FAIL $1 not ready after 5 minutes"; exit 1
}

wait_for keycloak "${KC%/protocol/*}"
wait_for app "$API/readyz"

CLERK=$(token clerk)
AUDITOR=$(token auditor)
check "clerk token" true "$([ ${#CLERK} -gt 100 ] && echo true || echo false)"
check "auditor token" true "$([ ${#AUDITOR} -gt 100 ] && echo true || echo false)"

check "livez" 200 "$(status "$API/livez")"
check "readyz" 200 "$(status "$API/readyz")"
check "no token" 401 "$(status "$API/api/contracts")"
check "search MOTOR (demo data)" 1 "$(curl -s -H "Authorization: Bearer $CLERK" "$API/api/contracts?lineOfBusiness=MOTOR" | jq '.page.totalElements')"
check "customer contracts (demo data)" true "$(curl -s -H "Authorization: Bearer $AUDITOR" "$API/api/customers/1/contracts" | jq '.page.totalElements >= 2')"

NUMBER="P-2026$(printf '%04d' $((RANDOM % 9000 + 1000)))"
BODY="{\"contractNumber\":\"$NUMBER\",\"customerId\":1,\"lineOfBusiness\":\"HOUSEHOLD\",\"premium\":12.90,\"startDate\":\"2026-01-01\"}"
post() { curl -s "$@" -X POST "$API/api/contracts" -H 'Content-Type: application/json'; }
HEADERS=$(mktemp)

check "auditor create" 403 "$(post -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $AUDITOR" -d "$BODY")"
RES=$(post -D "$HEADERS" -H "Authorization: Bearer $CLERK" -d "$BODY")
check "clerk create status" ACTIVE "$(echo "$RES" | jq -r .status)"
check "createdBy from JWT" clerk "$(echo "$RES" | jq -r .createdBy)"
check "Location header" "$API/api/contracts/$(echo "$RES" | jq .id)" "$(grep -i '^location:' "$HEADERS" | awk '{print $2}' | tr -d '\r')"
check "duplicate" 409 "$(post -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $CLERK" -d "$BODY")"
INVALID=$(post -H "Authorization: Bearer $CLERK" \
  -d '{"contractNumber":"X1","customerId":1,"lineOfBusiness":"MOTOR","premium":-5,"startDate":"2026-01-01","endDate":"2025-01-01"}')
check "invalid fields" '["contractNumber","endDate","premium"]' "$(echo "$INVALID" | jq -c '[.errors[].field] | sort')"

MCP_HEADERS=(-H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream')
MCP_INIT='{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"smoke","version":"1"}}}'
check "mcp without token" 401 "$(status -X POST "$API/mcp" "${MCP_HEADERS[@]}" -d "$MCP_INIT")"
SID=$(curl -s -D - -o /dev/null -X POST "$API/mcp" -H "Authorization: Bearer $AUDITOR" "${MCP_HEADERS[@]}" -d "$MCP_INIT" \
  | grep -i '^mcp-session-id:' | awk '{print $2}' | tr -d '\r')
mcp() { curl -s -X POST "$API/mcp" -H "Authorization: Bearer $AUDITOR" "${MCP_HEADERS[@]}" -H "Mcp-Session-Id: $SID" -d "$1" | sed -n 's/^data://p'; }
mcp '{"jsonrpc":"2.0","method":"notifications/initialized"}' > /dev/null
check "mcp tools" '["contract_details","contracts_of_customer","search_contracts","search_documents"]' \
  "$(mcp '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' | jq -c '[.result.tools[].name] | sort')"
check "mcp contract_details" true \
  "$(mcp '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"contract_details","arguments":{"contractNumber":"P-20260001"}}}' \
  | jq -r '.result.content[0].text' | grep -q 'P-20260001' && echo true || echo false)"

check "swagger" 200 "$(status -L "$API/swagger-ui.html")"

if [ "${SMOKE_RAG:-0}" = 1 ]; then
  DOCX="$(dirname "$0")/../samples/avb-glas-demo.docx"
  check "demo document imported" true "$(curl -s -H "Authorization: Bearer $AUDITOR" "$API/api/documents" | jq '.page.totalElements >= 1')"
  check "auditor upload" 403 "$(status -X POST "$API/api/documents" -H "Authorization: Bearer $AUDITOR" -F "file=@$DOCX")"
  DOC=$(curl -s -X POST "$API/api/documents" -H "Authorization: Bearer $CLERK" -F "file=@$DOCX")
  check "clerk upload docx" true "$(echo "$DOC" | jq '.chunks >= 1')"
  ANSWER=$(curl -s -m 300 -X POST "$API/api/documents/ask" -H "Authorization: Bearer $AUDITOR" -H 'Content-Type: application/json' \
    -d '{"question":"Ist ein gesprungenes Ceranfeld versichert?"}')
  check "rag answer" true "$(echo "$ANSWER" | jq '(.answer | length) > 0 and (.sources | length) > 0')"
  check "mcp search_documents" true \
    "$(mcp '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"search_documents","arguments":{"query":"Glasbruch"}}}' \
    | jq -r '.result.content[0].text' | grep -q 'filename' && echo true || echo false)"
  check "delete document" 204 "$(status -X DELETE "$API/api/documents/$(echo "$DOC" | jq .id)" -H "Authorization: Bearer $CLERK")"
fi

rm -f "$HEADERS"
[ $fail = 0 ] && echo "Smoke test passed" || echo "Smoke test FAILED"
exit $fail
