# ADR-002: Two bounded backend services

Status: accepted. Travel and Property have separate APIs, domain packages and migration ownership. This matches distinct planning concepts and permits independent hosting later. The operational cost is two local processes; no gateway, service discovery, broker or shared Java domain library is justified in Phase 1.
