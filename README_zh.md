<div align="center">

# ⚡ gatling-mvn-scala-test

### 四种压测类型，一条命令一个 —— Scala 版 Gatling 起手式仓库

基准、负载、压力、稳定性四种模拟，内置多环境配置、动态负载调整、系统监控、瓶颈分析与 HTML/JSON/CSV 报告。Fork 下来，指向你的 API，开跑。

[![gatling](https://img.shields.io/badge/gatling-3.8.4%20(sbt)%20%C2%B7%203.1.2%20(mvn)-blueviolet?style=flat-square)](#两种构建路径)
[![scala](https://img.shields.io/badge/scala-2.13%20%C2%B7%202.12-red?style=flat-square)](#两种构建路径)
[![simulations](https://img.shields.io/badge/simulations-4-blue?style=flat-square)](#-四种测试类型)
[![license](https://img.shields.io/badge/license-MIT-green?style=flat-square)](LICENSE)
[![stars](https://img.shields.io/github/stars/anthonyhann/gatling-mvn-scala-test?style=flat-square)](https://github.com/anthonyhann/gatling-mvn-scala-test/stargazers)

[快速开始](#-快速开始) · [测试类型](#-四种测试类型) · [真实数字](#-真实数字仓库可验证) · [目录结构](#-目录结构) · [什么时候别用](#-什么时候别用它) · [English](README.md)

</div>

## 你的第一个 Gatling 项目，大概是这样

- 从零写一个 simulation 要**接好几天的线**：配置、feeder、断言、监控、报告
- "dev 上跑得好好的"——一到别的环境，阈值、用户数、时长全都得重调
- 原生 HTML 报告告诉你*发生了什么*，却不告诉你*瓶颈在哪*

这个起手式仓库把线都接好了：4 个开箱即跑的 simulation 类、3 套环境配置，以及一整套配角（配置、feeder、监控、告警、错误分类、瓶颈分析、报告导出）全部插好。

## ✨ 里面有什么

| 部件 | 作用 |
|---|---|
| **4 种模拟** | `PrintServiceTest`（基准）· `PrintServiceLoadTest`（4 阶段负载）· `PrintServiceStressTest`（最高 2,000 并发）· `PrintServiceStabilityTest`（60 分钟浸泡） |
| **环境配置** | `dev` / `test` / `prod` 三个 properties——阈值、用户数、时长随 `-e` 一键切换 |
| **DynamicLoadAdjuster** | 按目标响应时间自动上下调整并发用户数，带最小/最大约束与完整调整历史 |
| **SystemMonitor + 告警** | CPU / 内存追踪 + 阈值告警（prod 默认：CPU 70% / 内存 80% / 错误率 5% / 响应时间 3,000ms），实时监控页跑在 `:8080` |
| **PerformanceBottleneckAnalyzer** | 分析响应时间趋势，标记疑似瓶颈，把优化建议写进报告 |
| **ErrorClassifier** | 按类型（超时 / 连接 / HTTP 状态码 / 解析）归类错误，输出数量、占比与单次报告 |
| **报告** | Gatling 原生 HTML + 增强版 HTML / JSON / CSV，落在 `target/gatling/` |

## 🚀 快速开始

```bash
# 1. 安装 sbt（脚本已内置）
./install-sbt.sh

# 2. 用 dev 环境跑基准测试
./run-test.sh benchmark        # 或：load | stress | stability

# 3. 打开报告——会自动在浏览器打开；
#    否则到 target/gatling/results/ 与 target/gatling/enhanced-reports/ 下找
```

**给 AI Agent 用**——把这句话粘贴给你的 Agent：

> 克隆 https://github.com/anthonyhann/gatling-mvn-scala-test，执行 `./install-sbt.sh` 然后 `./run-test.sh benchmark`，把报告路径给我。

要压自己的 API：改 `src/test/resources/environments/*.properties` 里的 `api.baseUrl` 与 `api.key`，再改 simulation 类里的请求逻辑。

## 🧪 四种测试类型

| 类型 | 类 | 目的 | prod 默认档位 |
|---|---|---|---|
| 基准 | `PrintServiceTest` | 正常负载下接口是否健康？日常回归 | 100 并发 × 6 分钟 |
| 负载 | `PrintServiceLoadTest` | 预期峰值流量扛不扛得住？ | 4 阶段，50→200 并发 × 40 分钟 |
| 压力 | `PrintServiceStressTest` | 极限在哪里？扩容依据 | 爬升至 2,000 并发，25 分钟 |
| 稳定性 | `PrintServiceStabilityTest` | 内存泄漏？长期运行退化？ | 1,100 并发 × 60 分钟 |

每套环境配置都会缩放档位（`dev` 更温和：基准 50 并发 × 3 分钟，压力上限 500 并发）。

## 两种构建路径

| | SBT（主，`run-test.sh` 走这条） | Maven（`run-base-test.sh`） |
|---|---|---|
| 文件 | `build.sbt` | `pom.xml` |
| Gatling | 3.8.4 | 3.1.2 |
| Scala | 2.13.8 | 2.12.10 |
| 运行 | `sbt "gatling:testOnly example.PrintServiceTest"` | `mvn gatling:test -Dgatling.simulationClass=example.PrintServiceTest` |

> 注意：`run-base-test.sh` 调用的是 `./mvnw`，但仓库只提交了 `.mvn/wrapper/maven-wrapper.jar`、没有提交 mvnw 脚本本身——请用系统 `mvn` 或走 SBT 路径。

## 📊 真实数字（仓库可验证）

- **28 个 Scala 文件**，约 6,200 行：4 个 simulation + `BasePerformanceTest` + `src/test/scala` 下 17 个支撑组件 + `src/main/scala` 下 6 个共享组件
- **3 套环境配置**（`dev` / `test` / `prod`），`test-config.properties` 共 **37 个配置项**
- **4 个 simulation 类**，覆盖基准 / 负载 / 压力 / 稳定性
- **3 种报告格式** —— HTML、JSON、CSV
- **3 个运行脚本** —— `run-test.sh`（类型快捷方式）、`run-base-test.sh`（Maven）、`install-sbt.sh`
- **2 套构建系统** —— SBT 与 Maven，均已预置 zh-CN 中文报告本地化

## 📂 目录结构

```
gatling-mvn-scala-test/
├── build.sbt / pom.xml            # SBT（Gatling 3.8.4）+ Maven（Gatling 3.1.2）双构建
├── run-test.sh                    # 一键运行：benchmark | load | stress | stability
├── run-base-test.sh               # Maven 方式运行
├── install-sbt.sh                 # sbt 安装脚本
└── src/
    ├── main/scala/example/        # 6 个共享组件（监控服务、告警、报告…）
    └── test/
        ├── scala/example/         # 4 个 simulation + BasePerformanceTest + 17 个组件
        │   ├── PrintServiceTest.scala           # 基准测试
        │   ├── PrintServiceLoadTest.scala       # 4 阶段负载测试
        │   ├── PrintServiceStressTest.scala     # 最高 2,000 并发压力测试
        │   ├── PrintServiceStabilityTest.scala  # 60 分钟稳定性测试
        │   ├── BasePerformanceTest.scala        # 共享 HTTP/配置/监控/报告基类
        │   ├── TestConfig.scala                 # 多环境配置加载
        │   ├── DynamicLoadAdjuster.scala        # 按响应时间动态调并发
        │   ├── ErrorClassifier.scala            # 错误归类 + 统计 + 报告
        │   ├── PerformanceBottleneckAnalyzer.scala
        │   ├── AlertManager.scala / SystemMonitor.scala / RealTimeMonitorServer.scala
        │   ├── TestReportExporter.scala / EnhancedReportGenerator.scala
        │   └── TestDataFeeder.scala / TestLauncher.scala / DebugHelper.scala / …
        └── resources/
            ├── test-config.properties           # 37 个默认配置项
            ├── environments/                    # dev / test / prod 环境配置
            ├── data/ + test_samples.csv         # CSV feeder 数据（请换成你自己的）
            └── templates/                       # 报告模板
```

## ⚠️ 什么时候别用它

- **它是起手式，不是库。** Fork 回去把 simulation 改成你的 API——没有东西可以 `import`。
- **只压你自己拥有或明确被授权压测的系统。** 内置配置指向一个占位 API；`prod` 配置怎么用是你的责任，包括法律与授权确认。
- **只覆盖协议级 HTTP 负载。** 不渲染浏览器、不执行 JS——要真实用户旅程请换浏览器驱动工具。
- **不是功能正确性测试。** 这里的断言验证的是性能阈值，不验证业务逻辑。
- **报告默认 zh-CN 中文本地化**（两套构建的 JVM 参数都写死了）——想要英文报告请改 `-Duser.language` / `-Duser.country`。


## 🙋 关于作者

从真实 API 压测工作中提炼——把每个新项目都要重接一遍的配置/feeder/监控/告警/报告管线固化成了起手式。如果它帮你省掉了这些搭建时间，⭐ 一下，帮下一位工程师找到它。

## License

MIT — 详见 [LICENSE](LICENSE)。
