# 三方服务API性能测试框架

这是一个用于测试三方服务API性能的Gatling测试框架，提供多种测试类型和丰富的功能，支持不同环境的配置和自动化测试执行。

## 框架特点

- **多种测试类型**：支持基准测试、负载测试、压力测试和稳定性测试
- **配置灵活**：使用属性文件进行配置，支持多环境配置
- **动态负载调整**：根据系统响应时间自动调整并发用户数
- **强大的监控**：支持CPU、内存等系统资源监控
- **性能瓶颈分析**：自动分析并提示可能的性能瓶颈
- **丰富的报告**：生成HTML、JSON、CSV等多种格式的报告
- **错误分类统计**：对错误进行分类和统计，帮助定位问题
- **告警机制**：支持多级别告警和多种通知方式
- **重试机制**：自动重试临时性错误
- **趋势分析**：支持性能指标的趋势分析和历史对比

## 目录结构

```
src/
├── test/
│   ├── resources/
│   │   ├── environments/        # 环境特定配置
│   │   │   ├── dev.properties   # 开发环境配置
│   │   │   ├── test.properties  # 测试环境配置
│   │   │   └── prod.properties  # 生产环境配置
│   │   └── test-config.properties  # 默认配置
│   └── scala/
│       └── example/            # 测试代码
│           ├── BasePerformanceTest.scala  # 测试基类
│           ├── PrintServiceTest.scala  # 优化版基准测试
│           ├── PrintServiceTest.scala  # 基准测试
│           ├── PrintServiceLoadTest.scala  # 负载测试
│           ├── PrintServiceStressTest.scala  # 压力测试
│           ├── PrintServiceStabilityTest.scala  # 稳定性测试
│           ├── AlertManager.scala  # 告警管理器
│           ├── DynamicLoadAdjuster.scala  # 动态负载调整器
│           ├── ErrorClassifier.scala  # 错误分类器
│           ├── PerformanceBottleneckAnalyzer.scala  # 性能瓶颈分析
│           ├── TestConfig.scala  # 配置管理
│           └── TestReportExporter.scala  # 报告导出工具
```

## 安装与运行

### 依赖项

- Java 8+
- Scala 2.12+
- Gatling 3.5+
- SBT 1.5+

### 安装SBT

如果尚未安装SBT，可以使用提供的安装脚本：

```bash
chmod +x install-sbt.sh
./install-sbt.sh
```

### 运行测试

使用提供的运行脚本可以轻松执行测试：

```bash
# 基本使用方式
./run-test.sh -t PrintServiceTest -e dev

# 更多选项
./run-test.sh --help
```

运行脚本支持以下选项：

- `-t, --test CLASS`：指定测试类（默认：PrintServiceTest）
- `-e, --env ENV`：指定环境（默认：dev）
- `-m, --monitor`：启用监控（默认：true）
- `-n, --no-monitor`：禁用监控
- `-d, --debug`：启用调试模式
- `-b, --no-browser`：不自动打开浏览器
- `-h, --help`：显示帮助信息

## 测试类型

### 基准测试 (PrintServiceTest)

验证单接口在正常负载下的性能表现，适用于日常开发验证。

**特点**：
- 稳定的并发用户数
- 适度的测试持续时间
- 基本的断言验证

### 负载测试 (PrintServiceLoadTest)

验证系统在预期峰值负载下的性能表现，测试系统是否能处理实际业务场景。

**特点**：
- 多阶段负载模式
- 稳定的各阶段持续时间
- 相对较长的测试时间
- 更详细的监控和报告

### 压力测试 (PrintServiceStressTest)

测试系统的极限承载能力，发现系统瓶颈，为系统扩容提供依据。

**特点**：
- 递增的极限负载
- 快速增长的并发用户
- 性能边界探测
- 系统资源监控

### 稳定性测试 (PrintServiceStabilityTest)

验证系统在持续高负载下的稳定性，发现内存泄漏等长期运行问题。

**特点**：
- 长时间持续高负载
- 资源监控和告警
- 记录无错误运行时间
- 系统恢复能力测试

## 核心组件说明

### BasePerformanceTest

所有测试类的基类，提供共享功能：

- HTTP协议配置
- 监控设置
- 响应处理和错误记录
- 测试前后钩子方法
- 报告生成

### TestConfig

配置管理组件，支持多环境配置：

- 加载默认配置和环境特定配置
- 提供类型安全的配置访问
- 支持配置缓存和重载

### DynamicLoadAdjuster

动态负载调整组件，根据系统响应调整测试负载：

- 根据响应时间自动调整并发用户数
- 支持最小、最大用户数限制
- 记录负载调整历史

### ErrorClassifier

错误分类和统计组件：

- 自动分类识别不同类型的错误
- 统计各类错误的数量和比例
- 生成错误分析报告

### AlertManager

告警管理和通知组件：

- 支持多种告警级别
- 支持不同的通知通道
- 控制告警静默期，避免告警风暴

### PerformanceBottleneckAnalyzer

性能瓶颈分析组件：

- 分析响应时间趋势
- 识别潜在性能瓶颈
- 提供优化建议

### TestReportExporter

测试报告导出组件：

- 支持HTML、JSON、CSV等格式
- 生成可视化图表
- 支持不同测试指标的导出

## 配置项

### 基础配置

配置文件位于`src/test/resources/test-config.properties`，包含了默认配置项。

### 环境特定配置

各环境的特定配置位于`src/test/resources/environments/`目录下：

- `dev.properties`: 开发环境 - 宽松的性能指标阈值，小规模测试
- `test.properties`: 测试环境 - 适中的性能指标阈值，中等规模测试
- `prod.properties`: 生产环境 - 严格的性能指标阈值，完整规模测试

### 主要配置项

- **API配置**
  - `api.baseUrl`: API基础URL
  - `api.key`: API认证密钥

- **性能指标阈值**
  - `performance.avgResponseTime`: 平均响应时间阈值(ms)
  - `performance.p95ResponseTime`: 95%响应时间阈值(ms)
  - `performance.p99ResponseTime`: 99%响应时间阈值(ms)
  - `performance.successRate`: 成功率阈值(%)
  - `performance.minTps`: 最小TPS阈值

- **监控设置**
  - `monitoring.enabled`: 是否启用监控
  - `monitoring.interval`: 监控间隔(秒)

- **告警阈值**
  - `alerts.cpuThreshold`: CPU告警阈值(%)
  - `alerts.memoryThreshold`: 内存告警阈值(%)
  - `alerts.errorRateThreshold`: 错误率告警阈值(%)
  - `alerts.responseTimeThreshold`: 响应时间告警阈值(ms)

- **重试设置**
  - `retry.maxAttempts`: 最大重试次数
  - `retry.intervalMs`: 重试间隔(ms)

- **动态负载调整**
  - `dynamicLoad.enabled`: 是否启用
  - `dynamicLoad.initialUsers`: 初始用户数
  - `dynamicLoad.maxUsers`: 最大用户数
  - `dynamicLoad.stepSize`: 调整步长
  - `dynamicLoad.targetResponseTime`: 目标响应时间(ms)
  - `dynamicLoad.adjustmentInterval`: 调整间隔(秒)

- **测试类型特定设置**
  - `PrintServiceTest.duration`: 基准测试持续时间
  - `PrintServiceTest.users`: 基准测试用户数
  - `PrintServiceLoadTest.stages.*`: 负载测试各阶段配置
  - `PrintServiceStressTest.maxUsers`: 压力测试最大用户数
  - `PrintServiceStabilityTest.users`: 稳定性测试用户数

## 测试结果与报告

测试完成后，报告将保存在`target/gatling/enhanced-reports/{testType}/`目录下，包含：

- `{testType}-report.html`: HTML格式报告，包含图表和详细统计
- `{testType}-report.json`: JSON格式报告，包含所有测试数据
- `{testType}-summary.csv`: CSV格式的测试摘要
- `{testType}-responseTime.csv`: 响应时间测试数据
- `{testType}-successRate.csv`: 成功率测试数据

## 扩展与自定义

### 创建自定义测试

可以通过继承`BasePerformanceTest`类来创建自定义测试：

```scala
class MyCustomTest extends BasePerformanceTest {
  // 实现必要的抽象成员
  override def testType: String = "MyCustomTest"
  override def testTitle: String = "自定义测试"
  override def testDescription: String = "自定义测试描述"
  
  // 实现executeRequest方法
  override protected def executeRequest(session: Session): Session = {
    // 实现请求逻辑
    // ...
    
    // 处理响应
    processResponse(session)
  }
  
  // 定义场景和负载模式
  // ...
}
```

### 添加新的告警通道

可以通过实现`NotificationChannel`特质来添加新的告警通道：

```scala
class MyNotificationChannel extends NotificationChannel {
  def sendNotification(alert: Alert): Unit = {
    // 实现发送通知的逻辑
  }
}

// 然后添加到AlertManager
AlertManager.addCustomNotification(new MyNotificationChannel())
```

## 故障排除

### 常见问题

1. **ConnectionRefused异常**
   - 问题：无法连接到API服务器
   - 解决：检查API地址配置和网络连接

2. **内存不足**
   - 问题：测试过程中JVM内存不足
   - 解决：增加JVM内存，例如`export SBT_OPTS="-Xmx4G"`

3. **响应解析错误**
   - 问题：无法正确解析API响应
   - 解决：检查API是否更改了响应格式，更新JSON路径

## 维护与贡献

欢迎提交问题报告和功能建议。如果您想贡献代码，请遵循以下步骤：

1. Fork 仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建Pull Request
