<div align="center">

# ⚡ gatling-mvn-scala-test

### Four load-test types, one command each — a Gatling starter kit in Scala

Benchmark, load, stress and stability simulations with per-environment profiles, dynamic load adjustment, system monitoring, bottleneck analysis and HTML/JSON/CSV reports. Fork it, point it at your API, run.

[![gatling](https://img.shields.io/badge/gatling-3.8.4%20(sbt)%20%C2%B7%203.1.2%20(mvn)-blueviolet?style=flat-square)](#two-build-paths)
[![scala](https://img.shields.io/badge/scala-2.13%20%C2%B7%202.12-red?style=flat-square)](#two-build-paths)
[![simulations](https://img.shields.io/badge/simulations-4-blue?style=flat-square)](#-four-test-types)
[![license](https://img.shields.io/badge/license-MIT-green?style=flat-square)](LICENSE)
[![stars](https://img.shields.io/github/stars/anthonyhann/gatling-mvn-scala-test?style=flat-square)](https://github.com/anthonyhann/gatling-mvn-scala-test/stargazers)

[Quickstart](#-quickstart) · [Test types](#-four-test-types) · [Numbers](#-numbers-verifiable-in-this-repo) · [Layout](#-repository-layout) · [Boundaries](#-when-not-to-use-it) · [中文版](README_zh.md)

</div>

## Your first Gatling project, probably

- Writing a simulation from scratch means **days of wiring**: config, feeders, assertions, monitoring, reports
- "Worked on dev" — then thresholds, user counts and durations all need to change per environment
- The stock HTML report tells you *what* happened, not *where the bottleneck* is

This starter kit ships the wiring: 4 ready-to-run simulation classes, 3 environment profiles, and the supporting cast (config, feeder, monitor, alerts, error classification, bottleneck analysis, report export) already plugged in.

## ✨ What's inside

| Piece | What it does |
|---|---|
| **4 simulation types** | `PrintServiceTest` (benchmark) · `PrintServiceLoadTest` (4-stage load) · `PrintServiceStressTest` (up to 2,000 users) · `PrintServiceStabilityTest` (60-min soak) |
| **Environment profiles** | `dev` / `test` / `prod` property files — thresholds, user counts and durations switch with `-e` |
| **DynamicLoadAdjuster** | Ramps concurrent users up/down toward a target response time, with min/max bounds and a full adjustment history |
| **SystemMonitor + alerts** | CPU & memory tracking with threshold alerts (prod defaults: CPU 70% / memory 80% / error rate 5% / RT 3,000 ms) and a real-time monitor page on `:8080` |
| **PerformanceBottleneckAnalyzer** | Analyzes response-time trends, flags likely bottlenecks, writes optimization hints into the report |
| **ErrorClassifier** | Buckets errors by type (timeout / connection / HTTP status / parse) with counts, ratios and a per-run report |
| **Reports** | Gatling HTML + enhanced HTML / JSON / CSV exports under `target/gatling/` |

## 🚀 Quickstart

```bash
# 1. Install sbt (script included)
./install-sbt.sh

# 2. Run the benchmark simulation against the dev profile
./run-test.sh benchmark        # or: load | stress | stability

# 3. Open the report — it auto-opens in your browser;
#    otherwise see target/gatling/results/ and target/gatling/enhanced-reports/
```

**For AI agents** — paste this to your agent:

> Clone https://github.com/anthonyhann/gatling-mvn-scala-test, run `./install-sbt.sh` then `./run-test.sh benchmark`, and give me the report path.

Point it at your own API by editing `src/test/resources/environments/*.properties` (`api.baseUrl`, `api.key`) and the request logic inside the simulation classes.

## 🧪 Four test types

| Type | Class | Purpose | Default prod profile |
|---|---|---|---|
| Benchmark | `PrintServiceTest` | Is the API healthy under normal load? Daily regression. | 100 users × 6 min |
| Load | `PrintServiceLoadTest` | Does it hold expected peak traffic? | 4 stages, 50→200 users × 40 min |
| Stress | `PrintServiceStressTest` | Where does it break? Capacity planning input. | ramp to 2,000 users, 25 min |
| Stability | `PrintServiceStabilityTest` | Memory leaks? Long-run degradation? | 1,100 users × 60 min |

Each environment profile scales these down (`dev` is gentler: 50 users × 3 min benchmark, 500-user stress cap).

## Two build paths

| | SBT (primary, used by `run-test.sh`) | Maven (`run-base-test.sh`) |
|---|---|---|
| File | `build.sbt` | `pom.xml` |
| Gatling | 3.8.4 | 3.1.2 |
| Scala | 2.13.8 | 2.12.10 |
| Run | `sbt "gatling:testOnly example.PrintServiceTest"` | `mvn gatling:test -Dgatling.simulationClass=example.PrintServiceTest` |

> Note: `run-base-test.sh` invokes `./mvnw`, but the wrapper script itself isn't committed (only `.mvn/wrapper/maven-wrapper.jar` is) — use system `mvn` or the SBT path.

## 📊 Numbers, verifiable in this repo

- **28 Scala files**, ~6,200 LOC: 4 simulations + `BasePerformanceTest` + 17 supporting components in `src/test/scala`, 6 shared components in `src/main/scala`
- **3 environment profiles** (`dev` / `test` / `prod`) with **37 config keys** in `test-config.properties`
- **4 simulation classes** covering benchmark / load / stress / stability
- **3 report formats** — HTML, JSON, CSV
- **3 run scripts** — `run-test.sh` (typed shortcuts), `run-base-test.sh` (Maven), `install-sbt.sh`
- **2 build systems** — SBT and Maven, both preconfigured with zh-CN report locale

## 📂 Repository layout

```
gatling-mvn-scala-test/
├── build.sbt / pom.xml            # SBT (Gatling 3.8.4) + Maven (Gatling 3.1.2) builds
├── run-test.sh                    # One-command runner: benchmark | load | stress | stability
├── run-base-test.sh               # Maven-based runner
├── install-sbt.sh                 # sbt bootstrap script
└── src/
    ├── main/scala/example/        # 6 shared components (monitor server, alerts, reports…)
    └── test/
        ├── scala/example/         # 4 simulations + BasePerformanceTest + 17 components
        │   ├── PrintServiceTest.scala           # benchmark
        │   ├── PrintServiceLoadTest.scala       # 4-stage load
        │   ├── PrintServiceStressTest.scala     # up to 2,000 users
        │   ├── PrintServiceStabilityTest.scala  # 60-min soak
        │   ├── BasePerformanceTest.scala        # shared HTTP/config/monitor/report plumbing
        │   ├── TestConfig.scala                 # multi-env config loader
        │   ├── DynamicLoadAdjuster.scala        # response-time-driven user ramping
        │   ├── ErrorClassifier.scala            # error bucketing + stats + report
        │   ├── PerformanceBottleneckAnalyzer.scala
        │   ├── AlertManager.scala / SystemMonitor.scala / RealTimeMonitorServer.scala
        │   ├── TestReportExporter.scala / EnhancedReportGenerator.scala
        │   └── TestDataFeeder.scala / TestLauncher.scala / DebugHelper.scala / …
        └── resources/
            ├── test-config.properties           # 37 default config keys
            ├── environments/                    # dev / test / prod profiles
            ├── data/ + test_samples.csv         # CSV feeders (replace with your own)
            └── templates/                       # report template
```

## ⚠️ When NOT to use it

- **It's a starter kit, not a library.** Fork it and rewrite the simulations for your API — there's nothing to `import`.
- **Only load-test systems you own or are explicitly allowed to test.** The bundled profiles point at a placeholder API; `prod` profile is your responsibility, including legal and ethical sign-off.
- **Protocol-level HTTP load only.** No browser rendering, no JS execution — for user-journey realism use a browser-driven tool instead.
- **Not a correctness test suite.** Assertions here validate performance thresholds, not business logic.
- **Reports are zh-CN-localized** via JVM args in both builds — flip `-Duser.language`/`-Duser.country` if you want English output.


## 🙋 About

Extracted from real API load-testing work — the config/feeder/monitor/alert/report wiring you would otherwise re-derive on every new project. If this starter kit saves you those setup days, a ⭐ helps the next engineer find it.

## License

MIT — see [LICENSE](LICENSE).
