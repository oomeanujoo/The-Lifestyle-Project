#!/usr/bin/env sh
set -eu
(cd travel-service && ./gradlew test)
(cd property-service && ./gradlew test)
(cd lifestyle-web && npm ci && npm run lint && npm test && npm run build)
