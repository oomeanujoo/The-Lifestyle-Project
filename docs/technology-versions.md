# Selected technology versions

Checked 2026-09-16 against official project release pages and npm package metadata. These are stable releases; no preview artifacts are used.

| Component | Selected | Evidence and reason |
|---|---:|---|
| Java | 25 LTS | [OpenJDK 25](https://openjdk.org/projects/jdk/25/) |
| Spring Boot | 4.1.1 | [Spring Boot project](https://spring.io/projects/spring-boot) |
| Gradle wrapper | 9.7.1 | [Releases](https://gradle.org/releases/); [Java 25 support](https://docs.gradle.org/current/userguide/compatibility.html) |
| PostgreSQL | 18.6 | [Supported versions](https://www.postgresql.org/support/versioning/) |
| Ollama image (optional) | 0.34.1 | [Published stable Docker tag](https://hub.docker.com/r/ollama/ollama/tags); only in Compose `ai` profile |
| Node.js | 24 LTS | [Release schedule](https://nodejs.org/en/about/previous-releases); Node 26 is Current, not LTS |
| React | 19.3.0 | [npm metadata](https://www.npmjs.com/package/react) |
| Vite | 8.3.0 | [npm metadata](https://www.npmjs.com/package/vite) |
| TypeScript | 6.0.3 | [npm metadata](https://www.npmjs.com/package/typescript); TypeScript 7.0.2 is newer, but the stable `typescript-eslint` peer range currently ends below 6.1 |
| Vitest | 5.0.1 | [npm metadata](https://www.npmjs.com/package/vitest) |
| React Router | 7.14.0 | [npm metadata](https://www.npmjs.com/package/react-router) |
| TanStack Query | 5.102.8 | [npm metadata](https://www.npmjs.com/package/%40tanstack/react-query) |
| springdoc-openapi (webmvc-ui) | 3.1.1 | [Maven Central metadata](https://repo1.maven.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/maven-metadata.xml), last published 2026-09-06; first stable release with explicit Spring Boot 4.1.x support |

Spring AI 2.0.x [supports Spring Boot 4.1.x](https://docs.spring.io/spring-ai/reference/getting-started.html). It is deferred because Phase 1 has no implemented AI call or local model requirement; adding it now would create a production dependency without a use case. The documented provider port is the intended Phase 2 integration point.

Local verification machine: Java 17, Node 22, Gradle 8.8 installed; Docker CLI present but Docker engine unavailable. Upgrade Java/Node to selected baselines before running builds. Runtime dependency downloads also require registry access.

Upgrade policy: keep Java 25 and Node 24 LTS until an explicitly reviewed LTS migration; check stable compatibility, pin Docker tags, update the npm lockfile with dependency changes, run builds/tests and record the change here. Do not use preview or floating `latest` releases.
