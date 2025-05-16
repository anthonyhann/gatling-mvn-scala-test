# Gatling Maven插件演示项目（Scala版本）

这个项目展示了如何使用Maven和Scala来配置和运行Gatling性能测试，并支持中文报告。

## 项目结构

- `src/test/scala/example/` - 包含Gatling测试场景
- `src/test/resources/` - 包含配置文件和测试数据
  - `gatling.conf` - Gatling全局配置
  - `io/gatling/core/i18n/` - 国际化文件

## 前提条件

- JDK 8或更高版本
- Maven 3.5或更高版本

## 运行测试

### 使用Maven命令运行

```bash
# 运行所有测试场景
mvn gatling:test

# 运行特定测试场景
mvn gatling:test -Dgatling.simulationClass=example.PrintServiceTest
```

### 运行中文报告测试

为了确保测试报告使用中文显示，你可以使用项目根目录下的脚本：

```bash
# 运行带中文报告的测试
./run-gatling-chinese.sh
```

或者直接使用以下Maven命令：

```bash
mvn clean gatling:test -Dgatling.simulationClass=example.PrintServiceTest \
  -Duser.language=zh \
  -Duser.country=CN \
  -Dgatling.core.i18n.lang=zh-CN \
  -Dfile.encoding=UTF-8
```

## 测试报告

测试完成后，将在以下目录生成报告：

```
target/gatling/{test-name-timestamp}/index.html
```

增强的测试报告将生成在：

```
target/gatling/enhanced-reports/{test-name}_report.html
```

## 支持的参数

运行测试时可以使用以下JVM参数：

- `vu`: 设置虚拟用户数（默认为1）
- `debug`: 启用调试模式，设置为"true"时启用（默认为"false"）
- `monitor`: 启用系统监控，设置为"true"时启用（默认为"true"）

示例：

```bash
mvn gatling:test -Dgatling.simulationClass=example.PrintServiceTest -Dvu=10 -Ddebug=true
```

## 国际化支持配置

为了支持中文报告，我们在项目中配置了以下内容：

1. 在`pom.xml`中添加了JVM参数：
```xml
<jvmArgs>
  <jvmArg>-Duser.language=zh</jvmArg>
  <jvmArg>-Duser.country=CN</jvmArg>
  <jvmArg>-Dgatling.core.i18n.lang=zh-CN</jvmArg>
  <jvmArg>-Dfile.encoding=UTF-8</jvmArg>
</jvmArgs>
```

2. 在`src/test/resources/gatling.conf`中添加了国际化设置：
```
i18n {
  lang = "zh"
}

locale {
  language = "zh"
  country = "CN"
}
```

3. 在`src/test/resources/io/gatling/core/i18n/`目录下添加了中文翻译文件：
   - `translations_zh.properties`
   - `translations_zh-CN.properties`

## 自定义报告

项目包含了Gatling的增强报告功能，可以生成更详细的测试报告和系统监控信息。这些自定义报告使用了以下组件：

- `ResponseLogger`: 记录原始响应日志
- `EnhancedReportGenerator`: 生成增强的HTML报告
- `SystemMonitor`: 监控系统资源使用情况

## 故障排除

如果测试报告未显示中文，请检查：

1. 确保JVM参数正确设置
2. 确保中文翻译文件位于正确的位置
3. 尝试使用`run-gatling-chinese.sh`脚本运行测试
