package example

import java.io.{File, FileWriter, PrintWriter}
import java.time.{LocalDateTime, Duration}
import java.time.format.DateTimeFormatter
import scala.collection.mutable.{Map => MutableMap, ArrayBuffer}
import scala.util.{Try, Success, Failure}

/**
 * 增强的报告生成器
 * 用于生成详细的性能测试报告，包括性能指标、趋势分析和自定义报告
 */
object EnhancedReportGenerator {
  private val reportDir = "target/gatling/enhanced-reports"
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  
  // 存储测试指标数据
  private val testMetrics = MutableMap[String, MutableMap[String, ArrayBuffer[Double]]]()
  private val testResults = MutableMap[String, MutableMap[String, Any]]()
  private val testHistories = MutableMap[String, ArrayBuffer[Map[String, Any]]]()
  
  // 测试开始和结束时间
  private val testStartTimes = MutableMap[String, LocalDateTime]()
  private val testEndTimes = MutableMap[String, LocalDateTime]()
  
  /**
   * 初始化测试报告
   * @param testName 测试名称
   * @param testType 测试类型（基准测试、负载测试等）
   * @param description 测试描述
   */
  def initializeReport(testName: String, testType: String, description: String): Unit = {
    ensureReportDirectory()
    
    // 初始化测试指标存储
    testMetrics.getOrElseUpdate(testName, MutableMap(
      "responseTime" -> ArrayBuffer[Double](),
      "successRate" -> ArrayBuffer[Double](),
      "requestsPerSecond" -> ArrayBuffer[Double]()
    ))
    
    // 记录测试信息
    testResults.getOrElseUpdate(testName, MutableMap(
      "testType" -> testType,
      "description" -> description,
      "startTime" -> LocalDateTime.now(),
      "status" -> "进行中"
    ))
    
    // 记录测试开始时间
    testStartTimes(testName) = LocalDateTime.now()
    
    println(s"${ANSI_GREEN}初始化增强报告 - 测试: $testName, 类型: $testType${ANSI_RESET}")
  }
  
  /**
   * 记录性能指标
   * @param testName 测试名称
   * @param metricName 指标名称
   * @param value 指标值
   */
  def recordMetric(testName: String, metricName: String, value: Double): Unit = {
    val testMetricsMap = testMetrics.getOrElseUpdate(testName, MutableMap())
    val metricsArray = testMetricsMap.getOrElseUpdate(metricName, ArrayBuffer[Double]())
    metricsArray.append(value)
  }
  
  /**
   * 记录多个性能指标
   * @param testName 测试名称
   * @param metrics 指标名称和值的映射
   */
  def recordMetrics(testName: String, metrics: Map[String, Double]): Unit = {
    metrics.foreach { case (name, value) =>
      recordMetric(testName, name, value)
    }
  }
  
  /**
   * 记录测试结果
   * @param testName 测试名称
   * @param key 结果键
   * @param value 结果值
   */
  def recordResult(testName: String, key: String, value: Any): Unit = {
    val testResultsMap = testResults.getOrElseUpdate(testName, MutableMap())
    testResultsMap(key) = value
  }
  
  /**
   * 完成测试报告
   * @param testName 测试名称
   * @param success 测试是否成功
   * @param message 完成消息
   */
  def finalizeReport(testName: String, success: Boolean, message: String = ""): Unit = {
    // 记录测试结束时间
    testEndTimes(testName) = LocalDateTime.now()
    
    // 更新测试结果
    val testResultsMap = testResults.getOrElseUpdate(testName, MutableMap())
    testResultsMap("endTime") = LocalDateTime.now()
    testResultsMap("duration") = formatDuration(Duration.between(
      testStartTimes.getOrElse(testName, LocalDateTime.now()),
      LocalDateTime.now()
    ))
    testResultsMap("success") = success
    testResultsMap("message") = message
    testResultsMap("status") = if (success) "成功" else "失败"
    
    // 保存当次测试结果到历史记录
    val historyRecord = Map(
      "testName" -> testName,
      "startTime" -> testStartTimes.getOrElse(testName, LocalDateTime.now()).format(dateTimeFormatter),
      "endTime" -> LocalDateTime.now().format(dateTimeFormatter),
      "success" -> success,
      "metrics" -> calculateMetricsSummary(testName)
    )
    testHistories.getOrElseUpdate(testName, ArrayBuffer[Map[String, Any]]()).append(historyRecord)
    
    // 生成HTML报告
    generateHtmlReport(testName)
    
    // 生成趋势分析报告
    generateTrendReport(testName)
    
    println(s"${ANSI_GREEN}完成增强报告 - 测试: $testName, 结果: ${if (success) "成功" else "失败"}${ANSI_RESET}")
  }
  
  /**
   * 计算指标摘要
   */
  private def calculateMetricsSummary(testName: String): Map[String, Map[String, String]] = {
    val metricsMap = testMetrics.getOrElse(testName, MutableMap())
    
    metricsMap.map { case (metricName, values) =>
      val avg = if (values.nonEmpty) values.sum / values.size else 0.0
      val max = if (values.nonEmpty) values.max else 0.0
      val min = if (values.nonEmpty) values.min else 0.0
      val p95 = if (values.nonEmpty) calculatePercentile(values.toArray, 95) else 0.0
      val p99 = if (values.nonEmpty) calculatePercentile(values.toArray, 99) else 0.0
      
      metricName -> Map(
        "avg" -> avg.formatted("%.2f"),
        "max" -> max.formatted("%.2f"),
        "min" -> min.formatted("%.2f"),
        "p95" -> p95.formatted("%.2f"),
        "p99" -> p99.formatted("%.2f")
      )
    }.toMap
  }
  
  /**
   * 计算百分位数
   */
  private def calculatePercentile(values: Array[Double], percentile: Int): Double = {
    if (values.isEmpty) return 0.0
    
    val sortedValues = values.sorted
    val index = math.ceil(percentile / 100.0 * sortedValues.length).toInt - 1
    val safeIndex = math.max(0, math.min(sortedValues.length - 1, index))
    sortedValues(safeIndex)
  }
  
  /**
   * 生成HTML报告
   */
  private def generateHtmlReport(testName: String): Unit = {
    val reportFileName = s"$reportDir/${testName}_report.html"
    val testResult = testResults.getOrElse(testName, MutableMap())
    val metricsSummary = calculateMetricsSummary(testName)
    
    // 附加系统监控数据（如果可用）
    val monitoringData = try {
      Some(SystemMonitor.getMonitoringDataForReport(testName))
    } catch {
      case _: Exception => None
    }
    
    val htmlContent = generateHtmlContent(testName, testResult.toMap, metricsSummary, monitoringData)
    
    Try {
      val writer = new PrintWriter(new FileWriter(reportFileName))
      try {
        writer.write(htmlContent)
      } finally {
        writer.close()
      }
      
      println(s"${ANSI_GREEN}生成HTML报告: $reportFileName${ANSI_RESET}")
    } match {
      case Success(_) => // 成功生成报告
      case Failure(e) => println(s"${ANSI_RED}生成HTML报告失败: ${e.getMessage}${ANSI_RESET}")
    }
  }
  
  /**
   * 生成趋势分析报告
   */
  private def generateTrendReport(testName: String): Unit = {
    val reportFileName = s"$reportDir/${testName}_trend.html"
    val historyRecords = testHistories.getOrElse(testName, ArrayBuffer())
    
    if (historyRecords.size < 2) {
      println(s"${ANSI_YELLOW}历史记录不足，无法生成趋势分析${ANSI_RESET}")
      return
    }
    
    val htmlContent = generateTrendHtmlContent(testName, historyRecords.toList)
    
    Try {
      val writer = new PrintWriter(new FileWriter(reportFileName))
      try {
        writer.write(htmlContent)
      } finally {
        writer.close()
      }
      
      println(s"${ANSI_GREEN}生成趋势分析报告: $reportFileName${ANSI_RESET}")
    } match {
      case Success(_) => // 成功生成报告
      case Failure(e) => println(s"${ANSI_RED}生成趋势分析报告失败: ${e.getMessage}${ANSI_RESET}")
    }
  }
  
  /**
   * 生成HTML内容
   */
  private def generateHtmlContent(
    testName: String, 
    testResult: Map[String, Any], 
    metricsSummary: Map[String, Map[String, String]],
    monitoringData: Option[Map[String, Any]]
  ): String = {
    val startTime = testResult.getOrElse("startTime", "未知").toString
    val endTime = testResult.getOrElse("endTime", "未知").toString
    val duration = testResult.getOrElse("duration", "未知").toString
    val success = testResult.getOrElse("success", false).asInstanceOf[Boolean]
    val testType = testResult.getOrElse("testType", "未知").toString
    val description = testResult.getOrElse("description", "").toString
    
    val metricsHtml = metricsSummary.map { case (metricName, values) =>
      s"""
        <tr>
          <td>${formatMetricName(metricName)}</td>
          <td>${values("avg")}</td>
          <td>${values("min")}</td>
          <td>${values("max")}</td>
          <td>${values("p95")}</td>
          <td>${values("p99")}</td>
        </tr>
      """
    }.mkString("\n")
    
    // 生成监控数据图表（如果有）
    val monitoringCharts = monitoringData.map { data =>
      val cpuData = data("cpuData").asInstanceOf[List[Map[String, Any]]]
      val memoryData = data("memoryData").asInstanceOf[List[Map[String, Any]]]
      
      if (cpuData.isEmpty && memoryData.isEmpty) {
        "<div>无监控数据可用</div>"
      } else {
        val cpuChartData = cpuData.map { point => 
          s"""{ time: "${point("time")}", value: ${point("value")} }"""
        }.mkString(", ")
        
        val memoryChartData = memoryData.map { point => 
          s"""{ time: "${point("time")}", value: ${point("value")} }"""
        }.mkString(", ")
        
        s"""
          <div class="monitoring-section">
            <h3>系统资源监控</h3>
            
            <div class="chart-container">
              <h4>CPU使用率 (%)</h4>
              <canvas id="cpuChart"></canvas>
            </div>
            
            <div class="chart-container">
              <h4>内存使用率 (%)</h4>
              <canvas id="memoryChart"></canvas>
            </div>
            
            <script>
              // CPU使用率图表
              var cpuCtx = document.getElementById('cpuChart').getContext('2d');
              var cpuData = [${cpuChartData}];
              
              var cpuChart = new Chart(cpuCtx, {
                type: 'line',
                data: {
                  labels: cpuData.map(item => item.time),
                  datasets: [{
                    label: 'CPU使用率 (%)',
                    data: cpuData.map(item => item.value),
                    borderColor: 'rgba(75, 192, 192, 1)',
                    backgroundColor: 'rgba(75, 192, 192, 0.2)',
                    tension: 0.1
                  }]
                },
                options: {
                  responsive: true,
                  scales: {
                    y: {
                      beginAtZero: true,
                      max: 100
                    }
                  }
                }
              });
              
              // 内存使用率图表
              var memoryCtx = document.getElementById('memoryChart').getContext('2d');
              var memoryData = [${memoryChartData}];
              
              var memoryChart = new Chart(memoryCtx, {
                type: 'line',
                data: {
                  labels: memoryData.map(item => item.time),
                  datasets: [{
                    label: '内存使用率 (%)',
                    data: memoryData.map(item => item.value),
                    borderColor: 'rgba(54, 162, 235, 1)',
                    backgroundColor: 'rgba(54, 162, 235, 0.2)',
                    tension: 0.1
                  }]
                },
                options: {
                  responsive: true,
                  scales: {
                    y: {
                      beginAtZero: true,
                      max: 100
                    }
                  }
                }
              });
            </script>
          </div>
        """
      }
    }.getOrElse("<div>未启用系统监控</div>")
    
    s"""
      <!DOCTYPE html>
      <html lang="zh-CN">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>${testName} - 性能测试报告</title>
        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
        <style>
          body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 20px;
            color: #333;
          }
          .container {
            max-width: 1200px;
            margin: 0 auto;
          }
          .header {
            background-color: #4CAF50;
            color: white;
            padding: 20px;
            border-radius: 5px;
            margin-bottom: 20px;
          }
          .success {
            background-color: #4CAF50;
          }
          .failure {
            background-color: #f44336;
          }
          .section {
            background-color: #f9f9f9;
            padding: 20px;
            border-radius: 5px;
            margin-bottom: 20px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
          }
          .monitoring-section {
            background-color: #f9f9f9;
            padding: 20px;
            border-radius: 5px;
            margin-bottom: 20px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
          }
          .chart-container {
            width: 100%;
            height: 300px;
            margin-bottom: 30px;
          }
          table {
            width: 100%;
            border-collapse: collapse;
          }
          th, td {
            border: 1px solid #ddd;
            padding: 8px;
            text-align: left;
          }
          th {
            background-color: #4CAF50;
            color: white;
          }
          tr:nth-child(even) {
            background-color: #f2f2f2;
          }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="header ${if (success) "success" else "failure"}">
            <h1>${testName} - 性能测试报告</h1>
            <p><strong>状态:</strong> ${if (success) "成功" else "失败"}</p>
          </div>
          
          <div class="section">
            <h2>测试信息</h2>
            <table>
              <tr><td>测试名称</td><td>${testName}</td></tr>
              <tr><td>测试类型</td><td>${testType}</td></tr>
              <tr><td>描述</td><td>${description}</td></tr>
              <tr><td>开始时间</td><td>${startTime}</td></tr>
              <tr><td>结束时间</td><td>${endTime}</td></tr>
              <tr><td>持续时间</td><td>${duration}</td></tr>
              <tr><td>结果</td><td>${if (success) "成功" else "失败"}</td></tr>
            </table>
          </div>
          
          <div class="section">
            <h2>性能指标</h2>
            <table>
              <thead>
                <tr>
                  <th>指标</th>
                  <th>平均值</th>
                  <th>最小值</th>
                  <th>最大值</th>
                  <th>95%</th>
                  <th>99%</th>
                </tr>
              </thead>
              <tbody>
                ${metricsHtml}
              </tbody>
            </table>
          </div>
          
          ${monitoringCharts}
          
          <div class="section">
            <h2>测试详情</h2>
            <p>在Gatling生成的HTML报告中查看完整的测试详情。</p>
            <p>报告路径: target/gatling/</p>
          </div>
        </div>
      </body>
      </html>
    """
  }
  
  /**
   * 生成趋势分析HTML内容
   */
  private def generateTrendHtmlContent(
    testName: String, 
    historyRecords: List[Map[String, Any]]
  ): String = {
    // 提取趋势数据
    val timestamps = historyRecords.map(_("startTime").toString)
    
    // 提取响应时间趋势
    val responseTimeTrend = historyRecords.map { record =>
      val metrics = record("metrics").asInstanceOf[Map[String, Map[String, String]]]
      metrics.getOrElse("responseTime", Map("avg" -> "0.0"))("avg").toDouble
    }
    
    // 提取成功率趋势
    val successRateTrend = historyRecords.map { record =>
      val metrics = record("metrics").asInstanceOf[Map[String, Map[String, String]]]
      metrics.getOrElse("successRate", Map("avg" -> "0.0"))("avg").toDouble
    }
    
    // 生成趋势图表数据
    val responseTimeData = timestamps.zip(responseTimeTrend).map { case (time, value) =>
      s"""{ time: "$time", value: $value }"""
    }.mkString(", ")
    
    val successRateData = timestamps.zip(successRateTrend).map { case (time, value) =>
      s"""{ time: "$time", value: $value }"""
    }.mkString(", ")
    
    s"""
      <!DOCTYPE html>
      <html lang="zh-CN">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>${testName} - 性能趋势分析</title>
        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
        <style>
          body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 20px;
            color: #333;
          }
          .container {
            max-width: 1200px;
            margin: 0 auto;
          }
          .header {
            background-color: #2196F3;
            color: white;
            padding: 20px;
            border-radius: 5px;
            margin-bottom: 20px;
          }
          .section {
            background-color: #f9f9f9;
            padding: 20px;
            border-radius: 5px;
            margin-bottom: 20px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
          }
          .chart-container {
            width: 100%;
            height: 300px;
            margin-bottom: 30px;
          }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="header">
            <h1>${testName} - 性能趋势分析</h1>
            <p>分析最近 ${historyRecords.size} 次测试的性能趋势</p>
          </div>
          
          <div class="section">
            <h2>响应时间趋势</h2>
            <div class="chart-container">
              <canvas id="responseTimeChart"></canvas>
            </div>
            
            <h2>成功率趋势</h2>
            <div class="chart-container">
              <canvas id="successRateChart"></canvas>
            </div>
            
            <script>
              // 响应时间趋势图表
              var responseTimeCtx = document.getElementById('responseTimeChart').getContext('2d');
              var responseTimeData = [${responseTimeData}];
              
              var responseTimeChart = new Chart(responseTimeCtx, {
                type: 'line',
                data: {
                  labels: responseTimeData.map(item => item.time),
                  datasets: [{
                    label: '平均响应时间 (ms)',
                    data: responseTimeData.map(item => item.value),
                    borderColor: 'rgba(75, 192, 192, 1)',
                    backgroundColor: 'rgba(75, 192, 192, 0.2)',
                    tension: 0.1
                  }]
                },
                options: {
                  responsive: true
                }
              });
              
              // 成功率趋势图表
              var successRateCtx = document.getElementById('successRateChart').getContext('2d');
              var successRateData = [${successRateData}];
              
              var successRateChart = new Chart(successRateCtx, {
                type: 'line',
                data: {
                  labels: successRateData.map(item => item.time),
                  datasets: [{
                    label: '成功率 (%)',
                    data: successRateData.map(item => item.value),
                    borderColor: 'rgba(54, 162, 235, 1)',
                    backgroundColor: 'rgba(54, 162, 235, 0.2)',
                    tension: 0.1
                  }]
                },
                options: {
                  responsive: true,
                  scales: {
                    y: {
                      beginAtZero: true,
                      max: 100
                    }
                  }
                }
              });
            </script>
          </div>
        </div>
      </body>
      </html>
    """
  }
  
  /**
   * 确保报告目录存在
   */
  private def ensureReportDirectory(): Unit = {
    val dir = new File(reportDir)
    if (!dir.exists()) {
      dir.mkdirs()
    }
  }
  
  /**
   * 格式化指标名称
   */
  private def formatMetricName(name: String): String = {
    name match {
      case "responseTime" => "响应时间 (ms)"
      case "successRate" => "成功率 (%)"
      case "requestsPerSecond" => "每秒请求数 (RPS)"
      case "throughput" => "吞吐量 (KB/s)"
      case "activeUsers" => "活跃用户数"
      case _ => name
    }
  }
  
  /**
   * 格式化持续时间
   */
  private def formatDuration(duration: Duration): String = {
    val hours = duration.toHours
    val minutes = (duration.toMinutes % 60)
    val seconds = (duration.getSeconds % 60)
    f"$hours%d小时 $minutes%d分钟 $seconds%d秒"
  }
  
  // ANSI颜色代码
  private val ANSI_RESET = "\u001B[0m"
  private val ANSI_RED = "\u001B[31m"
  private val ANSI_GREEN = "\u001B[32m"
  private val ANSI_YELLOW = "\u001B[33m"
} 