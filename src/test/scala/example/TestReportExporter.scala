package example

import java.io.{File, FileWriter, PrintWriter}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.{Try, Success, Failure}

/**
 * 测试报告导出工具
 * 负责将测试结果导出为各种格式：HTML、JSON、CSV等
 */
object TestReportExporter {
  // 报告保存目录
  private val ReportBaseDir = "target/gatling/enhanced-reports"
  
  /**
   * 导出测试报告
   * @param testType 测试类型
   * @param formats 导出格式列表，支持"html", "json", "csv"
   */
  def exportReports(testType: String, formats: List[String]): Unit = {
    // 创建报告目录
    val reportDir = new File(s"$ReportBaseDir/$testType")
    if (!reportDir.exists()) {
      reportDir.mkdirs()
    }
    
    // 获取测试结果数据
    val testResults = EnhancedReportGenerator.getAllResults(testType)
    val testMetrics = EnhancedReportGenerator.getAllMetrics(testType)
    
    // 按格式导出
    formats.foreach { format =>
      format.toLowerCase match {
        case "html" => exportHtmlReport(testType, testResults, testMetrics)
        case "json" => exportJsonReport(testType, testResults, testMetrics)
        case "csv" => exportCsvReport(testType, testResults, testMetrics)
        case _ => println(s"不支持的报告格式: $format")
      }
    }
    
    println(s"报告已导出到 $reportDir")
  }
  
  /**
   * 导出HTML报告
   */
  private def exportHtmlReport(
    testType: String, 
    results: Map[String, Any], 
    metrics: Map[String, List[Double]]
  ): Unit = {
    val file = new File(s"$ReportBaseDir/$testType/$testType-report.html")
    val writer = new PrintWriter(new FileWriter(file))
    
    try {
      writer.println("<!DOCTYPE html>")
      writer.println("<html>")
      writer.println("<head>")
      writer.println("<meta charset=\"UTF-8\">")
      writer.println(s"<title>${testType} 测试报告</title>")
      writer.println("<style>")
      writer.println("body { font-family: Arial, sans-serif; margin: 20px; }")
      writer.println("h1 { color: #333366; }")
      writer.println("h2 { color: #336699; margin-top: 30px; }")
      writer.println("table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }")
      writer.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
      writer.println("th { background-color: #f2f2f2; }")
      writer.println("tr:nth-child(even) { background-color: #f9f9f9; }")
      writer.println(".success { color: green; }")
      writer.println(".failure { color: red; }")
      writer.println(".chart { width: 100%; height: 300px; margin-bottom: 30px; }")
      writer.println("</style>")
      // 添加Chart.js库
      writer.println("<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>")
      writer.println("</head>")
      writer.println("<body>")
      
      // 标题
      writer.println(s"<h1>${testType} 测试报告</h1>")
      writer.println(s"<p>生成时间: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}</p>")
      
      // 测试概要
      writer.println("<h2>测试概要</h2>")
      writer.println("<table>")
      writer.println("<tr><th>参数</th><th>值</th></tr>")
      
      List(
        "testStartTime" -> "测试开始时间",
        "testEndTime" -> "测试结束时间",
        "actualDuration" -> "测试持续时间",
        "totalRequests" -> "总请求数",
        "successfulRequests" -> "成功请求数",
        "failedRequests" -> "失败请求数",
        "overallSuccessRate" -> "总体成功率"
      ).foreach { case (key, label) =>
        val value = results.getOrElse(key, "N/A")
        writer.println(s"<tr><td>$label</td><td>$value</td></tr>")
      }
      
      writer.println("</table>")
      
      // 性能指标
      writer.println("<h2>性能指标</h2>")
      
      // 响应时间图表
      if (metrics.contains("responseTime") && metrics("responseTime").nonEmpty) {
        writer.println("<h3>响应时间</h3>")
        writer.println("<div class=\"chart\"><canvas id=\"responseTimeChart\"></canvas></div>")
        
        // 准备图表数据
        val responseTimeData = metrics("responseTime")
        val dataPoints = responseTimeData.length
        val labels = (1 to dataPoints).map(i => s"$i").mkString(",")
        val data = responseTimeData.mkString(",")
        
        // 图表脚本
        writer.println("<script>")
        writer.println("var ctx = document.getElementById('responseTimeChart').getContext('2d');")
        writer.println("var chart = new Chart(ctx, {")
        writer.println("    type: 'line',")
        writer.println("    data: {")
        writer.println(s"        labels: [$labels],")
        writer.println("        datasets: [{")
        writer.println("            label: '响应时间 (ms)',")
        writer.println(s"            data: [$data],")
        writer.println("            borderColor: 'rgb(75, 192, 192)',")
        writer.println("            tension: 0.1")
        writer.println("        }]")
        writer.println("    },")
        writer.println("    options: {")
        writer.println("        scales: {")
        writer.println("            y: {")
        writer.println("                beginAtZero: true")
        writer.println("            }")
        writer.println("        }")
        writer.println("    }")
        writer.println("});")
        writer.println("</script>")
      }
      
      // 成功率图表
      if (metrics.contains("successRate") && metrics("successRate").nonEmpty) {
        writer.println("<h3>成功率</h3>")
        writer.println("<div class=\"chart\"><canvas id=\"successRateChart\"></canvas></div>")
        
        // 准备图表数据
        val successRateData = metrics("successRate")
        val dataPoints = successRateData.length
        val labels = (1 to dataPoints).map(i => s"$i").mkString(",")
        val data = successRateData.mkString(",")
        
        // 图表脚本
        writer.println("<script>")
        writer.println("var ctx = document.getElementById('successRateChart').getContext('2d');")
        writer.println("var chart = new Chart(ctx, {")
        writer.println("    type: 'line',")
        writer.println("    data: {")
        writer.println(s"        labels: [$labels],")
        writer.println("        datasets: [{")
        writer.println("            label: '成功率 (%)',")
        writer.println(s"            data: [$data],")
        writer.println("            borderColor: 'rgb(54, 162, 235)',")
        writer.println("            tension: 0.1")
        writer.println("        }]")
        writer.println("    },")
        writer.println("    options: {")
        writer.println("        scales: {")
        writer.println("            y: {")
        writer.println("                beginAtZero: true,")
        writer.println("                max: 100")
        writer.println("            }")
        writer.println("        }")
        writer.println("    }")
        writer.println("});")
        writer.println("</script>")
      }
      
      // 错误分析
      if (results.contains("bottleneckAnalysis")) {
        writer.println("<h2>性能瓶颈分析</h2>")
        writer.println("<pre>")
        writer.println(results("bottleneckAnalysis"))
        writer.println("</pre>")
      }
      
      // 错误分析
      if (results.contains("errorClassificationReport")) {
        writer.println("<h2>错误分类报告</h2>")
        writer.println("<pre>")
        writer.println(results("errorClassificationReport"))
        writer.println("</pre>")
      }
      
      // 结束HTML
      writer.println("</body>")
      writer.println("</html>")
    } finally {
      writer.close()
    }
  }
  
  /**
   * 导出JSON报告
   */
  private def exportJsonReport(
    testType: String, 
    results: Map[String, Any], 
    metrics: Map[String, List[Double]]
  ): Unit = {
    val file = new File(s"$ReportBaseDir/$testType/$testType-report.json")
    val writer = new PrintWriter(new FileWriter(file))
    
    try {
      // 合并结果和指标
      val jsonData = Map(
        "testType" -> testType,
        "results" -> results,
        "metrics" -> metrics
      )
      
      // 转换为JSON
      writer.println(toJson(jsonData))
    } finally {
      writer.close()
    }
  }
  
  /**
   * 导出CSV报告
   */
  private def exportCsvReport(
    testType: String, 
    results: Map[String, Any], 
    metrics: Map[String, List[Double]]
  ): Unit = {
    // 导出汇总结果
    val summaryFile = new File(s"$ReportBaseDir/$testType/$testType-summary.csv")
    val summaryWriter = new PrintWriter(new FileWriter(summaryFile))
    
    try {
      // 写入标题行
      summaryWriter.println("Metric,Value")
      
      // 写入结果
      results.foreach { case (key, value) =>
        // 跳过复杂数据类型
        value match {
          case _: Map[_, _] => // 跳过Map
          case _: List[_] => // 跳过List
          case _ => summaryWriter.println(s"$key,$value")
        }
      }
    } finally {
      summaryWriter.close()
    }
    
    // 导出指标数据
    metrics.foreach { case (metricName, values) =>
      if (values.nonEmpty) {
        val metricFile = new File(s"$ReportBaseDir/$testType/$testType-$metricName.csv")
        val metricWriter = new PrintWriter(new FileWriter(metricFile))
        
        try {
          // 写入标题行
          metricWriter.println("Index,Value")
          
          // 写入数据
          values.zipWithIndex.foreach { case (value, index) =>
            metricWriter.println(s"${index + 1},$value")
          }
        } finally {
          metricWriter.close()
        }
      }
    }
  }
  
  /**
   * 简单的Map转JSON实现
   */
  private def toJson(data: Any): String = {
    data match {
      case m: Map[_, _] => 
        val entries = m.map { case (k, v) => s"\"$k\": ${toJson(v)}" }
        s"{${entries.mkString(", ")}}"
      case l: List[_] => 
        val elements = l.map(toJson)
        s"[${elements.mkString(", ")}]"
      case d: Double => d.toString
      case i: Int => i.toString
      case l: Long => l.toString
      case b: Boolean => b.toString
      case s: String => s"\"${escapeJson(s)}\""
      case null => "null"
      case _ => s"\"${data.toString}\""
    }
  }
  
  /**
   * 转义JSON字符串
   */
  private def escapeJson(s: String): String = {
    s.replace("\\", "\\\\")
     .replace("\"", "\\\"")
     .replace("\n", "\\n")
     .replace("\r", "\\r")
     .replace("\t", "\\t")
  }
} 