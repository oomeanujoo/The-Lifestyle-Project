#!/usr/bin/env sh
set -eu
docker compose up -d postgres
printf 'Start each backend in its own terminal: (cd travel-service && ./gradlew bootRun), (cd property-service && ./gradlew bootRun), (cd integration-service && ./gradlew bootRun)\n'
printf 'Start frontend: (cd lifestyle-web && npm install && npm run dev)\n'
