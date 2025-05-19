package example

import java.io.{File, FileWriter, PrintWriter}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.collection.mutable.{Map => MutableMap, ArrayBuffer, Set => MutableSet}
import scala.collection.concurrent.TrieMap

/**
 * 增强报告生成器
 * 负责收集测试数据并生成详细的测试报告
 */
object EnhancedReportGenerator {
  // 测试结果数据
  private val testResults = new TrieMap[String, MutableMap[String, Any]]()
  
  // 性能指标数据
  private val metricsData = new TrieMap[String, MutableMap[String, ArrayBuffer[Double]]]()
  
  // 活跃测试集合
  private val activeTests = MutableSet[String]()
  
  // 报告输出目录
  private val ReportBaseDir = "target/gatling/enhanced-reports"
  
  // 日期格式化
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
  
  /**
   * 初始化报告
   * @param testType 测试类型
   * @param title 测试标题
   * @param description 测试描述
   */
  def initializeReport(testType: String, title: String, description: String): Unit = {
    // 确保目录存在
    ensureDirectoryExists(ReportBaseDir)
    
    // 创建测试结果数据结构
    val testResultMap = testResults.getOrElseUpdate(testType, MutableMap[String, Any]())
    
    // 记录测试基础信息
    testResultMap += ("testType" -> testType)
    testResultMap += ("title" -> title)
    testResultMap += ("description" -> description)
    testResultMap += ("startTime" -> System.currentTimeMillis())
    testResultMap += ("status" -> "进行中")
    testResultMap += ("environment" -> System.getProperty("test.env", "未指定"))
    
    // 初始化性能指标数组
    metricsData.getOrElseUpdate(testType, MutableMap[String, ArrayBuffer[Double]]())
    
    // 添加到活跃测试
    activeTests += testType
    
    println(s"初始化测试报告: $testType")
  }
  
  /**
   * 记录测试结果
   * @param testType 测试类型
   * @param key 结果键
   * @param value 结果值
   * @return 之前的值，如果没有则返回空字符串
   */
  def recordResult(testType: String, key: String, value: Any): Any = {
    val testResultMap = testResults.getOrElseUpdate(testType, MutableMap[String, Any]())
    val prev = testResultMap.getOrElse(key, "")
    testResultMap += (key -> value)
    prev
  }
  
  /**
   * 记录性能指标
   * @param testType 测试类型
   * @param metrics 性能指标Map
   */
  def recordMetrics(testType: String, metrics: Map[String, Double]): Unit = {
    val metricsMap = metricsData.getOrElseUpdate(testType, MutableMap[String, ArrayBuffer[Double]]())
    
    // 添加新指标
    metrics.foreach { case (key, value) =>
      val metricsArray = metricsMap.getOrElseUpdate(key, ArrayBuffer[Double]())
      metricsArray.synchronized {
        metricsArray += value
        
        // 限制数据点数量，保持最新的2000个点
        if (metricsArray.size > 2000) {
          metricsMap(key) = metricsArray.takeRight(2000)
        }
      }
    }
    
    // 更新到实时监控
    if (RealTimeMonitorServer.isRunning) {
      RealTimeMonitorServer.addPerformanceMetrics(testType, metrics)
    }
  }
  
  /**
   * 获取最近的性能指标
   * @param testType 测试类型
   * @return 最新的性能指标Map
   */
  def getRecentMetrics(testType: String): Map[String, Double] = {
    val metricsMap = metricsData.getOrElse(testType, MutableMap[String, ArrayBuffer[Double]]())
    
    metricsMap.map { case (key, values) =>
      if (values.nonEmpty) (key -> values.last)
      else (key -> 0.0)
    }.toMap
  }
  
  /**
   * 获取指定指标的最近几个数据点
   * @param testType 测试类型
   * @param metricName 指标名称
   * @param count 数据点数量
   * @return 最近的数据点列表
   */
  def getRecentMetrics(testType: String, metricName: String, count: Int): List[Double] = {
    val metricsMap = metricsData.getOrElse(testType, MutableMap[String, ArrayBuffer[Double]]())
    
    metricsMap.get(metricName) match {
      case Some(values) => values.takeRight(count).toList
      case None => List()
    }
  }
  
  /**
   * 获取测试摘要信息
   * @param testType 测试类型
   * @return 测试摘要Map
   */
  def getTestSummary(testType: String): Map[String, Any] = {
    testResults.getOrElse(testType, MutableMap()).toMap
  }
  
  /**
   * 获取所有活跃测试类型
   * @return 活跃测试类型列表
   */
  def getActiveTests: List[String] = {
    activeTests.toList
  }
  
  /**
   * 获取所有测试结果
   * @param testType 测试类型
   * @return 所有测试结果
   */
  def getAllResults(testType: String): Map[String, Any] = {
    testResults.getOrElse(testType, MutableMap()).toMap
  }
  
  /**
   * 获取所有指标数据
   * @param testType 测试类型
   * @return 所有指标数据
   */
  def getAllMetrics(testType: String): Map[String, List[Double]] = {
    val metricsMap = metricsData.getOrElse(testType, MutableMap[String, ArrayBuffer[Double]]())
    metricsMap.map { case (key, values) => (key -> values.toList) }.toMap
  }
  
  /**
   * 完成报告生成
   * @param testType 测试类型
   * @param success 测试是否成功
   * @param summary 测试总结
   */
  def finalizeReport(testType: String, success: Boolean, summary: String): Unit = {
    val testResultMap = testResults.getOrElse(testType, MutableMap[String, Any]())
    
    // 记录结束时间
    testResultMap += ("endTime" -> System.currentTimeMillis())
    testResultMap += ("status" -> (if (success) "成功" else "失败"))
    testResultMap += ("summary" -> summary)
    
    // 计算测试持续时间
    val startTime = testResultMap.getOrElse("startTime", 0L).asInstanceOf[Long]
    val endTime = testResultMap("endTime").asInstanceOf[Long]
    val durationMs = endTime - startTime
    val durationFormatted = formatDuration(durationMs)
    testResultMap += ("duration" -> durationFormatted)
    
    // 生成HTML报告
    generateHtmlReport(testType)
    
    // 生成JSON报告
    generateJsonReport(testType)
    
    // 从活跃测试中移除
    activeTests -= testType
    
    println(s"完成测试报告: $testType, 状态: ${testResultMap("status")}")
  }
  
  /**
   * 生成HTML报告
   * @param testType 测试类型
   */
  private def generateHtmlReport(testType: String): Unit = {
    val testResultMap = testResults.getOrElse(testType, MutableMap[String, Any]())
    
    // 创建报告目录
    val now = LocalDateTime.now()
    val timestamp = now.format(dateTimeFormatter)
    val reportDir = new File(s"$ReportBaseDir/$testType-$timestamp")
    reportDir.mkdirs()
    
    // 创建HTML文件
    val htmlFile = new File(reportDir, "report.html")
    val writer = new PrintWriter(new FileWriter(htmlFile))
    
    try {
      // 生成HTML内容
      val html = generateHtmlContent(testType, testResultMap.toMap)
      writer.println(html)
      } finally {
        writer.close()
      }
      
    println(s"HTML报告已生成: ${htmlFile.getAbsolutePath}")
  }
  
  /**
   * 生成JSON报告
   * @param testType 测试类型
   */
  private def generateJsonReport(testType: String): Unit = {
    val testResultMap = testResults.getOrElse(testType, MutableMap[String, Any]())
    
    // 创建报告目录
    val now = LocalDateTime.now()
    val timestamp = now.format(dateTimeFormatter)
    val reportDir = new File(s"$ReportBaseDir/$testType-$timestamp")
    reportDir.mkdirs()
    
    // 创建JSON文件
    val jsonFile = new File(reportDir, "report.json")
    val writer = new PrintWriter(new FileWriter(jsonFile))
    
    try {
      // 生成JSON内容
      val json = generateJsonContent(testResultMap.toMap)
      writer.println(json)
      } finally {
        writer.close()
      }
      
    println(s"JSON报告已生成: ${jsonFile.getAbsolutePath}")
  }
  
  /**
   * 生成HTML内容
   * @param testType 测试类型
   * @param data 测试数据
   * @return HTML内容
   */
  private def generateHtmlContent(testType: String, data: Map[String, Any]): String = {
    val title = data.getOrElse("title", "性能测试").toString
    val description = data.getOrElse("description", "").toString
    val status = data.getOrElse("status", "未知").toString
    val statusClass = if (status == "成功") "success" else if (status == "失败") "danger" else "warning"
    val summary = data.getOrElse("summary", "").toString
    val environment = data.getOrElse("environment", "未指定").toString
    val duration = data.getOrElse("duration", "").toString
    
    // 获取测试开始和结束时间
    val startTime = data.getOrElse("startTime", 0L).asInstanceOf[Long]
    val endTime = data.getOrElse("endTime", 0L).asInstanceOf[Long]
    val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val startTimeFormatted = LocalDateTime.ofInstant(
      java.time.Instant.ofEpochMilli(startTime),
      java.time.ZoneId.systemDefault()
    ).format(dateFormat)
    val endTimeFormatted = LocalDateTime.ofInstant(
      java.time.Instant.ofEpochMilli(endTime),
      java.time.ZoneId.systemDefault()
    ).format(dateFormat)
    
    // 获取请求统计
    val totalRequests = data.getOrElse("totalRequests", 0).toString
    val successfulRequests = data.getOrElse("successfulRequests", 0).toString
    val failedRequests = data.getOrElse("failedRequests", 0).toString
    val successRate = data.getOrElse("overallSuccessRate", 0.0).asInstanceOf[Double]
    
    // 生成HTML
    s"""
      <!DOCTYPE html>
      <html lang="zh-CN">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>$title - 性能测试报告</title>
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css">
        <style>
          body { padding: 20px; }
          .card { margin-bottom: 20px; }
          .status-badge { font-size: 1.2em; }
          .summary-box { 
            padding: 15px; 
            border-radius: 5px;
            margin-bottom: 20px;
            background-color: #f8f9fa;
            border-left: 5px solid #6c757d;
          }
          .data-table { width: 100%; }
          .data-table th { width: 30%; }
        </style>
      </head>
      <body>
        <div class="container">
          <h1 class="mb-4">$title <span class="badge bg-$statusClass status-badge">$status</span></h1>
          <p class="lead">$description</p>
          
          <div class="summary-box">
            <h4>测试总结</h4>
            <p>$summary</p>
          </div>
          
          <div class="row">
            <div class="col-md-6">
              <div class="card">
                <div class="card-header">
                  <h5>测试信息</h5>
                </div>
                <div class="card-body">
                  <table class="table data-table">
                    <tr>
                      <th>测试类型</th>
                      <td>$testType</td>
                    </tr>
                    <tr>
                      <th>环境</th>
                      <td>$environment</td>
                    </tr>
                    <tr>
                      <th>开始时间</th>
                      <td>$startTimeFormatted</td>
                    </tr>
                    <tr>
                      <th>结束时间</th>
                      <td>$endTimeFormatted</td>
                    </tr>
                    <tr>
                      <th>持续时间</th>
                      <td>$duration</td>
                    </tr>
                  </table>
                </div>
              </div>
            </div>
            
            <div class="col-md-6">
              <div class="card">
                <div class="card-header">
                  <h5>请求统计</h5>
                </div>
                <div class="card-body">
                  <table class="table data-table">
                    <tr>
                      <th>总请求数</th>
                      <td>$totalRequests</td>
                    </tr>
                    <tr>
                      <th>成功请求</th>
                      <td>$successfulRequests</td>
                    </tr>
                    <tr>
                      <th>失败请求</th>
                      <td>$failedRequests</td>
                    </tr>
                    <tr>
                      <th>成功率</th>
                      <td>${successRate.formatted("%.2f")}%</td>
                    </tr>
            </table>
                </div>
              </div>
            </div>
          </div>
          
          <div class="card">
            <div class="card-header">
              <h5>详细数据</h5>
            </div>
            <div class="card-body">
              <table class="table table-striped">
              <thead>
                <tr>
                    <th>参数</th>
                    <th>值</th>
                </tr>
              </thead>
              <tbody>
                  ${generateDataRows(data)}
              </tbody>
            </table>
          </div>
          </div>
        </div>
      </body>
      </html>
    """
  }
  
  /**
   * 生成数据行
   * @param data 测试数据
   * @return 数据行HTML
   */
  private def generateDataRows(data: Map[String, Any]): String = {
    val sb = new StringBuilder
    
    // 排除已在其他部分显示的字段
    val excludedKeys = Set("testType", "title", "description", "startTime", "endTime", 
                           "status", "summary", "duration", "totalRequests", 
                           "successfulRequests", "failedRequests", "overallSuccessRate")
    
    // 处理剩余数据
    for ((key, value) <- data.toSeq.sortBy(_._1)) {
      if (!excludedKeys.contains(key)) {
        // 格式化值
        val formattedValue = formatValue(value)
        sb.append(s"""
          <tr>
            <td>$key</td>
            <td>$formattedValue</td>
          </tr>
        """)
      }
    }
    
    sb.toString
  }
  
  /**
   * 格式化值
   * @param value 原始值
   * @return 格式化后的字符串
   */
  private def formatValue(value: Any): String = {
    value match {
      case map: Map[_, _] => 
        val formattedMap = map.map { case (k, v) => s"$k: ${formatValue(v)}" }.mkString("<br>")
        s"<div>$formattedMap</div>"
      case list: List[_] => 
        val formattedList = list.map(item => formatValue(item)).mkString("<br>")
        s"<div>$formattedList</div>"
      case array: Array[_] => 
        val formattedArray = array.map(item => formatValue(item)).mkString("<br>")
        s"<div>$formattedArray</div>"
      case d: Double => f"$d%.2f"
      case _ => value.toString
    }
  }
  
  /**
   * 生成JSON内容
   * @param data 测试数据
   * @return JSON字符串
   */
  private def generateJsonContent(data: Map[String, Any]): String = {
    val jsonBuilder = new StringBuilder
    jsonBuilder.append("{\n")
    
    val sortedData = data.toSeq.sortBy(_._1)
    for (i <- sortedData.indices) {
      val (key, value) = sortedData(i)
      jsonBuilder.append(s"""  "$key": ${valueToJson(value)}""")
      if (i < sortedData.size - 1) {
        jsonBuilder.append(",\n")
      }
    }
    
    jsonBuilder.append("\n}")
    jsonBuilder.toString
  }
  
  /**
   * 将值转换为JSON字符串
   * @param value 原始值
   * @return JSON字符串表示
   */
  private def valueToJson(value: Any): String = {
    value match {
      case null => "null"
      case s: String => s""""$s""""
      case n: Number => n.toString
      case b: Boolean => b.toString
      case m: Map[_, _] => mapToJson(m.asInstanceOf[Map[String, Any]])
      case l: List[_] => listToJson(l)
      case a: Array[_] => listToJson(a.toList)
      case _ => s""""${value.toString}""""
    }
  }
  
  /**
   * 将Map转换为JSON字符串
   * @param map Map对象
   * @return JSON字符串表示
   */
  private def mapToJson(map: Map[String, Any]): String = {
    val jsonPairs = map.map { case (key, value) =>
      s""""$key": ${valueToJson(value)}"""
    }
    s"{${jsonPairs.mkString(", ")}}"
  }
  
  /**
   * 将List转换为JSON字符串
   * @param list List对象
   * @return JSON字符串表示
   */
  private def listToJson(list: List[_]): String = {
    val jsonValues = list.map(item => valueToJson(item))
    s"[${jsonValues.mkString(", ")}]"
  }
  
  /**
   * 格式化持续时间
   * @param durationMs 毫秒数
   * @return 格式化的持续时间字符串
   */
  private def formatDuration(durationMs: Long): String = {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60))
    
    f"$hours%d小时 $minutes%d分钟 $seconds%d秒"
  }
  
  /**
   * 确保目录存在
   * @param path 目录路径
   */
  private def ensureDirectoryExists(path: String): Unit = {
    val dir = new File(path)
    if (!dir.exists()) {
      dir.mkdirs()
    }
  }
} 