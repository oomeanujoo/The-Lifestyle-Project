# Phase 0 environment audit

Read-only inspection on 2026-09-16 in the existing Mega-synchronised repository. Measurements are a momentary snapshot; no Docker, PostgreSQL, Ollama, IDE installer or system service was started, and no database connection was attempted. No secrets or personal paths are recorded here.

## System and active memory

| Item | Observed |
|---|---|
| Windows | Pro, release 25H2, build 26220.9223. The registry reports `Windows 10 Pro`; Gradle identifies the OS as Windows 11. |
| CPU | Intel Core i7-7700HQ at 2.80 GHz; 8 logical processors detected. Intel specifies [4 physical cores and 8 threads](https://www.intel.com/content/www/us/en/products/compare.html?productIds=97185); direct Windows core enumeration was access-denied. |
| RAM | 15.9 GiB total; 2.8 GiB available when sampled. |
| Project drive | C:; approximately 127–137 GiB free across two read-only measurements. |
| Page file | Windows automatic setting `?:\pagefile.sys`; performance counter showed about 0.8% usage. Allocated size was not readable without the denied management interface. |
| Hardware virtualization | Enablement could not be established from the permitted read-only interfaces. No firmware or Windows setting was changed. |

The largest grouped working sets at audit time were VS Code (`Code`, 12 processes, ~2.5 GiB), ChatGPT (~1.6 GiB), Chrome (~1.6 GiB), Brave (~0.9 GiB), and Node (13 processes, ~0.6 GiB). Working set is resident memory, not a process limit, and these values change. IntelliJ was not running. The available RAM is tight for running both IDEs, two backends, Docker Desktop and a local model together.

## Installed versus required

| Component | Installed/detected | Project target | Recommendation | Approval required |
|---|---|---|---|---|
| Java JDK | Oracle 17.0.10 selected; Java 8 also installed; no JDK 25 found | 25 LTS | Add JDK 25 side-by-side and select it only in this project's terminal/toolchain | Yes, before installation or `JAVA_HOME`/PATH change |
| Gradle | Both checked-in wrappers 9.7.1; launcher and daemon currently use JDK 17 | 9.7.1 with Java 25 toolchain | Keep wrapper; install JDK 25 before backend tests | No repo version change; JDK action needs approval |
| Node.js | 22.15.1; no nvm/fnm/Volta detected | 24 LTS | Add Node 24 side-by-side without replacing 22 | Yes |
| npm | 11.4.0; `npm config get offline` returned `true` | 11.x | Keep; use project-local `npm ci` and override offline mode for that command only if cache misses | No global upgrade |
| VS Code | CLI 1.136.2; installed-app registry 1.137.0; 59 extension directories, some duplicate versions | Compatible stable | Keep; use workspace exclusions and one main IDE at a time | Yes for application upgrade/global settings |
| IntelliJ IDEA Community | 2024.1.1; installed VM options include 2 GiB maximum heap; not running | Compatible stable | Keep; measure when used before changing heap or plugins | Yes for upgrade/global tuning |
| Spring Tool Suite | 5.1.1 RELEASE folder detected | Compatible stable | Keep; no measured need to change | Yes for upgrade/global tuning |
| PostgreSQL tools | `psql` and `postgres` 18.0 under PostgreSQL 18 installation, not on PATH | Compose image 18.6 for future full-stack checks | No database action in Phase 0; distinguish local tools from future container | Yes before service/config changes |
| Docker | Docker Desktop 4.40.0 installed; CLI 28.0.4 | Future optional local infrastructure | Leave stopped during Phase 0 | Yes before starting/changing Windows services |
| Ollama | No executable or standard installation detected | Optional Compose `ai` profile only | Do not start or download models | Yes before any installation/model download |
| Git | 2.45.0.windows.1 | No pinned version | Keep | No |

`where.exe java` resolved Oracle Java shims; `java` and `javac` both report 17.0.10. `JAVA_HOME` points to the installed JDK 17. `where.exe node` resolves the installed Node 22 first. The frontend lockfile exists, and `package.json` now declares Node `>=24 <25` and npm `>=11 <12`. No project `.npmrc` or Node version manager was detected. The installed PostgreSQL binaries were version-queried without connecting to a server.

## Compatibility and loopback diagnosis

Java 25 is missing, so neither service can compile against its declared Java 25 toolchain. Node 22 is outside the declared Node 24 LTS engine range, although the existing frontend previously built under Node 22 with an engine warning. The local PostgreSQL executable is 18.0 while the future Compose image is 18.6; no compatibility conclusion requires a database connection now.

Both wrappers report Gradle 9.7.1 correctly, but earlier build attempts failed before project compilation with `Unable to establish loopback connection`. Existing Gradle daemon logs show Java NIO `PipeImpl`/`WEPollSelectorImpl` failing in `UnixDomainSockets.connect0` with `Invalid argument: connect`; the error appeared under installed Gradle 8.8 and wrapper 9.7.1. This points to the local Java/Windows execution or sandbox environment, not a demonstrated Spring Boot source error. The exact cause is unconfirmed. Do not alter the firewall, registry or network configuration. After JDK 25 is approved and selected, retry one wrapper with `--no-daemon --stacktrace`; if it still fails in a normal local terminal, compare that diagnostic before considering system changes.

## Approximate memory budget

These are planning ranges for this machine, not reservations. Java heap limits exclude native memory and Gradle/IDE processes.

| Component | Approximate RAM while active | Basis |
|---|---:|---|
| Windows and background services | 3–5 GiB | Planning baseline; current background load varies |
| VS Code | 1–2.5 GiB | Upper end measured at ~2.5 GiB with 12 processes; 59 extension directories are installed, not necessarily all active |
| IntelliJ IDEA | 2–3.5 GiB | Installed maximum heap 2 GiB plus native/indexing overhead; not running during audit |
| React/Vite and TypeScript tools | 0.4–1 GiB | Planning range; depends on dev-server and editor activity |
| One Spring Boot service | 0.7–1.1 GiB | `bootRun` heap capped at 512 MiB plus native memory |
| Two Spring Boot services | 1.4–2.2 GiB | Two service processes; Gradle daemons add separate overhead |
| PostgreSQL | 0.2–0.8 GiB | Future local development estimate; not started |
| Docker Desktop | 0.8–2 GiB | Future overhead estimate; not started |
| Optional local AI model | ~1.5 GiB to well above 8 GiB | Model, context and runtime dependent; not started or downloaded |

Do not downgrade an IDE based only on memory use. First inspect which projects are open, active extension/plugin load, indexing and generated-directory exclusions, IDE heap, and background processes. Current evidence favors using **one main IDE at a time**, closing unneeded browser tabs or Node processes, and keeping AI off during ordinary development. The existing IntelliJ heap ceiling is 2 GiB; no global IDE setting was changed. The number of open projects and actual IntelliJ plugin load were not measured without launching it.

## Workflows

**Light development now:** keep Docker, PostgreSQL and Ollama stopped. Use VS Code or IntelliJ, not both. Run the frontend alone after selecting Node 24: `cd lifestyle-web; npm ci; npm run dev`. For checks without a dev server: `npm run lint`, `npm test`, `npm run build`. Backend editing can proceed, but backend startup and tests wait for JDK 25 and a database in a later approved verification session. The repository-only Gradle settings cap workers at two, disable parallel builds, cap daemon heap at 512 MiB, expire idle daemons after two minutes and retain build caching. Workspace VS Code settings exclude generated files and cap the Java/TypeScript language-server heaps.

**Full-stack verification later:** after explicit approval and the runtime prerequisites, start PostgreSQL only, then test Travel and Property one at a time with `./gradlew test --no-daemon --stacktrace`; start one backend, check health/info, stop it, then repeat for the other. Run the frontend lint/test/build. Only run both backends together for the final cross-service check. Leave the optional Ollama profile stopped unless a separate AI test is requested. Do not automatically download a model.

## System-wide approval checkpoint

No action below was executed. These are proposed actions for separate approval; sizes are estimates and should be checked against the selected archive before installing.

| Proposed installer/action | Why | Disk and memory impact | Other-project impact | Rollback | Administrator permission |
|---|---|---|---|---|---|
| Download [Temurin `OpenJDK25U-jdk_x64_windows_hotspot_25.0.4.1_1.zip`](https://github.com/adoptium/temurin25-binaries/releases/tag/jdk-25.0.4.1%2B1), verify its published SHA-256, and extract **side-by-side** under `%LOCALAPPDATA%\Programs\TheLifestyleTools`; select it only in a project terminal | Required Java 25 compilation | Roughly 0.4–0.8 GiB disk; no persistent RAM when idle | None if Java 17 and global PATH remain unchanged; session selection affects only that terminal | Remove the new JDK directory and close the project terminal; keep Java 17 | Not expected for a user-owned archive; approval required regardless |
| Download [Node `node-v24.21.0-win-x64.zip`](https://nodejs.org/download/release/v24.21.0/), verify against `SHASUMS256.txt`, and extract side-by-side under `%LOCALAPPDATA%\Programs\TheLifestyleTools`; select it only in a project terminal | Required Node 24 engine | Roughly 0.1–0.3 GiB disk; no persistent RAM when idle | None if Node 22 and global PATH remain unchanged; session selection affects only that terminal | Remove the new Node directory and close the project terminal | Not expected for a user-owned archive; approval required regardless |
| If the Gradle loopback error persists outside the managed environment, investigate Windows networking/Java compatibility based on a new stacktrace | Backend verification | Unknown until diagnosed | Potentially broad if a system setting is involved | Restore any separately approved setting from a recorded prior value | May require administrator permission; no change is proposed yet |

Before either installation, confirm the official checksum and the exact extraction destination, then seek approval. No Java/Node installation, PATH or `JAVA_HOME` change, registry edit, service start, IDE upgrade, extension removal, or Mega setting change is authorized by this audit.
