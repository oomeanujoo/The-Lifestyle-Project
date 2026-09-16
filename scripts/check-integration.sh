#!/usr/bin/env sh
# Run this on the machine where integration-service is actually running —
# it checks each external API path independently, so a failure at step 2
# vs. step 3 tells you whether the problem is GeoNames credentials
# specifically, or something broader (network egress, integration-service
# itself down). See TECHNICAL_ARCHITECTURE.md §17.3/§18/§20.
set -u

BASE="${INTEGRATION_BASE_URL:-http://localhost:8083}"

echo "=== 1. integration-service reachable at all? ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" "$BASE/api/integration/v1/info" || echo "FAILED TO CONNECT"
curl -s "$BASE/api/integration/v1/info"
echo

echo "=== 2. GeoNames (needs GEONAMES_USERNAME set on integration-service) ==="
curl -s "$BASE/api/integration/v1/masters/geonames/search?q=Pune"
echo

echo "=== 3. Frankfurter (needs NO credentials — should always work) ==="
curl -s "$BASE/api/integration/v1/masters/frankfurter/latest?base=INR&symbols=USD,EUR,GBP"
echo

echo "=== 4. AI provider status ==="
curl -s "$BASE/api/integration/v1/ai/providers/status"
echo

echo "=== 5. External data client health (last real hit) ==="
curl -s "$BASE/api/integration/v1/masters/health"
echo

echo "=== 6. From travel-service's own container, can IT reach integration-service? ==="
echo "Run separately with your real container/service name, e.g.:"
echo "  docker exec <travel-service-container-name> curl -s http://integration-service:8083/api/integration/v1/info"
echo "(replace 'integration-service' with whatever your compose file names that service)"
