package example

import java.io.{File, FileWriter, PrintWriter}
import java.net.InetSocketAddress
import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import scala.collection.mutable.{Map => MutableMap, ArrayBuffer}
import scala.util.{Try, Success, Failure}

import com.sun.net.httpserver.{HttpServer, HttpHandler, HttpExchange}

/**
 * 实时监控服务器
 * 提供Web界面展示测试进度和系统资源指标
 */
object RealTimeMonitorServer {
  private val server = {
    val s = HttpServer.create(new InetSocketAddress(8080), 0)
    s.setExecutor(Executors.newCachedThreadPool())
    s
  }
  
  private val dataRefreshInterval = 2 // 数据刷新间隔（秒）
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  
  // 测试数据缓存
  private val testStats = MutableMap[String, MutableMap[String, Any]]()
  private val performanceMetrics = MutableMap[String, ArrayBuffer[(Long, Map[String, Double])]]()
  private val systemMetrics = MutableMap[String, ArrayBuffer[(Long, Map[String, Double])]]()
  private val alertsBuffer = ArrayBuffer[Alert]() // 告警缓存
  
  // 是否已启动
  private var running = false
  private var dataCollector: Option[ScheduledExecutorService] = None
  
  /**
   * 获取服务器运行状态
   * @return 是否正在运行
   */
  def isRunning: Boolean = running
  
  /**
   * 启动监控服务器
   * @param port 服务器端口，默认8080
   */
  def start(port: Int = 8080): Unit = {
    if (running) {
      println(s"监控服务器已经在运行中，访问地址: http://localhost:$port")
      return
    }
    
    // 创建路由处理
    server.createContext("/", new DashboardHandler())
    server.createContext("/api/stats", new StatsApiHandler())
    server.createContext("/api/system", new SystemMetricsApiHandler())
    server.createContext("/api/performance", new PerformanceMetricsApiHandler())
    server.createContext("/api/alerts", new AlertsApiHandler())
    
    // 启动服务器
    server.start()
    running = true
    
    // 启动数据收集器
    startDataCollector()
    
    println(s"实时监控服务器已启动，访问地址: http://localhost:$port")
  }
  
  /**
   * 停止监控服务器
   */
  def stop(): Unit = {
    if (!running) {
      return
    }
    
    // 停止数据收集器
    dataCollector.foreach { executor =>
      executor.shutdown()
      try {
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          executor.shutdownNow()
        }
      } catch {
        case _: InterruptedException => 
          executor.shutdownNow()
      }
    }
    
    // 停止服务器
    server.stop(0)
    running = false
    println("实时监控服务器已停止")
  }
  
  /**
   * 更新测试状态
   * @param testName 测试名称
   * @param stats 测试统计数据
   */
  def updateTestStats(testName: String, stats: Map[String, Any]): Unit = {
    val testStatsMap = testStats.getOrElseUpdate(testName, MutableMap())
    stats.foreach { case (key, value) =>
      testStatsMap(key) = value
    }
  }
  
  /**
   * 添加性能指标数据点
   * @param testName 测试名称
   * @param metrics 性能指标Map
   */
  def addPerformanceMetrics(testName: String, metrics: Map[String, Double]): Unit = {
    val metricsBuffer = performanceMetrics.getOrElseUpdate(testName, ArrayBuffer())
    metricsBuffer.append((System.currentTimeMillis(), metrics))
    
    // 保持最多1000个数据点
    if (metricsBuffer.size > 1000) {
      performanceMetrics(testName) = metricsBuffer.takeRight(1000)
    }
  }
  
  /**
   * 添加系统指标数据点
   * @param metrics 系统指标Map
   */
  def addSystemMetrics(metrics: Map[String, Double]): Unit = {
    val metricsBuffer = systemMetrics.getOrElseUpdate("system", ArrayBuffer())
    metricsBuffer.append((System.currentTimeMillis(), metrics))
    
    // 保持最多1000个数据点
    if (metricsBuffer.size > 1000) {
      systemMetrics("system") = metricsBuffer.takeRight(1000)
    }
  }
  
  /**
   * 添加告警
   * @param alert 告警对象
   */
  def addAlert(alert: Alert): Unit = {
    synchronized {
      alertsBuffer.append(alert)
      
      // 只保留最近100条告警
      if (alertsBuffer.size > 100) {
        alertsBuffer.remove(0, alertsBuffer.size - 100)
      }
    }
  }
  
  /**
   * 获取最近的告警
   * @param limit 返回的最大告警数量
   * @return 告警列表
   */
  def getRecentAlerts(limit: Int = 20): List[Alert] = {
    synchronized {
      alertsBuffer.sortBy(-_.timestamp).take(limit).toList
    }
  }
  
  /**
   * 启动数据收集器
   */
  private def startDataCollector(): Unit = {
    val executor = Executors.newScheduledThreadPool(1)
    executor.scheduleAtFixedRate(() => collectData(), 0, dataRefreshInterval, TimeUnit.SECONDS)
    dataCollector = Some(executor)
  }
  
  /**
   * 收集测试数据和系统指标
   */
  private def collectData(): Unit = {
    try {
      // 收集系统指标
      val cpuUsage = SystemMonitor.getCpuUsage
      val memoryUsage = SystemMonitor.getMemoryUsage
      val systemMetricsMap = Map(
        "cpu" -> cpuUsage,
        "memory" -> memoryUsage
      )
      addSystemMetrics(systemMetricsMap)
      
      // 收集活跃测试的性能指标
      EnhancedReportGenerator.getActiveTests.foreach { testName =>
        val metrics = EnhancedReportGenerator.getRecentMetrics(testName)
        if (metrics.nonEmpty) {
          addPerformanceMetrics(testName, metrics)
        }
        
        // 更新测试统计信息
        val testStats = EnhancedReportGenerator.getTestSummary(testName)
        updateTestStats(testName, testStats)
      }
    } catch {
      case e: Exception => println(s"收集监控数据时出错: ${e.getMessage}")
    }
  }
  
  /**
   * 仪表盘HTML处理器
   */
  private class DashboardHandler extends HttpHandler {
    def handle(exchange: HttpExchange): Unit = {
      val response = generateDashboardHtml()
      exchange.getResponseHeaders.add("Content-Type", "text/html; charset=UTF-8")
      exchange.sendResponseHeaders(200, response.length)
      val os = exchange.getResponseBody
      os.write(response.getBytes("UTF-8"))
      os.close()
    }
    
    /**
     * 生成仪表盘HTML
     */
    private def generateDashboardHtml(): String = {
      val sb = new StringBuilder
      
      sb.append("""
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>性能测试实时监控</title>
          <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css">
          <style>
            body { padding: 20px; }
            .card { margin-bottom: 20px; }
            .status-running { color: #28a745; }
            .status-completed { color: #17a2b8; }
            .status-failed { color: #dc3545; }
            .alert-item { padding: 10px; margin-bottom: 5px; border-radius: 4px; }
            .alert-info { background-color: #d1ecf1; }
            .alert-warning { background-color: #fff3cd; }
            .alert-error { background-color: #f8d7da; }
            .alert-critical { background-color: #dc3545; color: white; }
          </style>
        </head>
        <body>
          <div class="container">
            <h1 class="mb-4">性能测试实时监控</h1>
            <p>数据每<span id="refresh-seconds">2</span>秒自动刷新</p>
            
            <div class="row mb-4">
              <div class="col-md-12">
                <div class="card">
                  <div class="card-header">
                    <h5>系统资源监控</h5>
                  </div>
                  <div class="card-body">
                    <div class="row">
                      <div class="col-md-6">
                        <h5>CPU使用率</h5>
                        <div class="progress mb-3">
                          <div id="cpu-bar" class="progress-bar" role="progressbar" style="width: 0%;">0%</div>
                        </div>
                      </div>
                      <div class="col-md-6">
                        <h5>内存使用率</h5>
                        <div class="progress mb-3">
                          <div id="memory-bar" class="progress-bar bg-info" role="progressbar" style="width: 0%;">0%</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="row">
              <div class="col-md-8">
                <div class="card">
                  <div class="card-header">
                    <h5>活跃测试</h5>
                  </div>
                  <div class="card-body" id="active-tests">
                    <p>加载中...</p>
                  </div>
                </div>
              </div>
              
              <div class="col-md-4">
                <div class="card">
                  <div class="card-header">
                    <h5>告警面板</h5>
                  </div>
                  <div class="card-body">
                    <div id="alerts-container">
                      <p>加载中...</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <script>
            // 自动刷新
            function fetchData() {
              // 获取系统指标
              fetch('/api/system')
                .then(response => response.json())
                .then(data => {
                  if (data.current) {
                    updateSystemMetrics(data.current);
                  }
                })
                .catch(error => console.error('获取系统指标失败:', error));
              
              // 获取测试状态
              fetch('/api/stats')
                .then(response => response.json())
                .then(data => {
                  updateActiveTests(data);
                })
                .catch(error => console.error('获取测试状态失败:', error));
              
              // 获取告警
              fetch('/api/alerts')
                .then(response => response.json())
                .then(data => {
                  updateAlerts(data);
                })
                .catch(error => console.error('获取告警失败:', error));
            }
            
            // 更新系统指标
            function updateSystemMetrics(metrics) {
              const cpuBar = document.getElementById('cpu-bar');
              const memoryBar = document.getElementById('memory-bar');
              
              if (metrics.cpu !== undefined) {
                const cpuValue = Math.round(metrics.cpu);
                cpuBar.style.width = cpuValue + '%';
                cpuBar.textContent = cpuValue + '%';
                
                // 设置颜色
                if (cpuValue > 80) {
                  cpuBar.className = 'progress-bar bg-danger';
                } else if (cpuValue > 60) {
                  cpuBar.className = 'progress-bar bg-warning';
                } else {
                  cpuBar.className = 'progress-bar bg-success';
                }
              }
              
              if (metrics.memory !== undefined) {
                const memoryValue = Math.round(metrics.memory);
                memoryBar.style.width = memoryValue + '%';
                memoryBar.textContent = memoryValue + '%';
                
                // 设置颜色
                if (memoryValue > 80) {
                  memoryBar.className = 'progress-bar bg-danger';
                } else if (memoryValue > 60) {
                  memoryBar.className = 'progress-bar bg-warning';
                } else {
                  memoryBar.className = 'progress-bar bg-info';
                }
              }
            }
            
            // 更新活跃测试
            function updateActiveTests(data) {
              const container = document.getElementById('active-tests');
              
              if (!data || Object.keys(data).length === 0) {
                container.innerHTML = '<p>当前没有活跃的测试</p>';
                return;
              }
              
              let html = '';
              
              for (const [testName, stats] of Object.entries(data)) {
                const status = stats.status || 'unknown';
                const statusClass = status === '进行中' ? 'status-running' : 
                                   (status === '成功' ? 'status-completed' : 'status-failed');
                
                html += `
                  <div class="test-card mb-4">
                    <h5>${testName} <span class="${statusClass}">(${status})</span></h5>
                    <div class="row mb-2">
                      <div class="col-md-4">
                        <strong>开始时间:</strong> ${stats.testStartTime || '-'}
                      </div>
                      <div class="col-md-4">
                        <strong>持续时间:</strong> ${stats.actualDuration || '-'}
                      </div>
                      <div class="col-md-4">
                        <strong>环境:</strong> ${stats.environment || '-'}
                      </div>
                    </div>
                    <div class="row">
                      <div class="col-md-3">
                        <strong>总请求数:</strong> ${stats.totalRequests || '0'}
                      </div>
                      <div class="col-md-3">
                        <strong>成功请求:</strong> ${stats.successfulRequests || '0'}
                      </div>
                      <div class="col-md-3">
                        <strong>失败请求:</strong> ${stats.failedRequests || '0'}
                      </div>
                      <div class="col-md-3">
                        <strong>成功率:</strong> ${stats.overallSuccessRate ? stats.overallSuccessRate.toFixed(2) + '%' : '-'}
                      </div>
                    </div>
                  </div>
                `;
              }
              
              container.innerHTML = html;
            }
            
            // 更新告警
            function updateAlerts(data) {
              const container = document.getElementById('alerts-container');
              
              if (!data.alerts || data.alerts.length === 0) {
                container.innerHTML = '<p>当前没有告警</p>';
                return;
              }
              
              let html = '<h6>最近告警:</h6>';
              
              for (const alert of data.alerts) {
                let alertClass = 'alert-info';
                if (alert.level === 'WARNING') alertClass = 'alert-warning';
                if (alert.level === 'ERROR') alertClass = 'alert-error';
                if (alert.level === 'CRITICAL') alertClass = 'alert-critical';
                
                html += `
                  <div class="alert-item ${alertClass}">
                    <div><strong>${alert.level}</strong> - ${alert.formattedTime}</div>
                    <div>${alert.message}</div>
                    <div><small>${alert.testType}</small></div>
                  </div>
                `;
              }
              
              container.innerHTML = html;
            }
            
            // 首次加载数据
            fetchData();
            
            // 设置定时刷新
            setInterval(fetchData, 2000);
          </script>
        </body>
        </html>
      """)
      
      sb.toString
    }
  }
  
  /**
   * 测试统计数据API处理器
   */
  private class StatsApiHandler extends HttpHandler {
    def handle(exchange: HttpExchange): Unit = {
      val response = generateStatsJson()
      exchange.getResponseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(200, response.length)
      val os = exchange.getResponseBody
      os.write(response.getBytes("UTF-8"))
      os.close()
    }
    
    /**
     * 生成统计数据的JSON表示
     * @return JSON字符串
     */
    private def generateStatsJson(): String = {
      // 构建测试结果数据
      val testStatsJson = new StringBuilder
      testStatsJson.append("[")
      val statsEntries = testStats.toList
      for (i <- statsEntries.indices) {
        val (testName, stats) = statsEntries(i)
        testStatsJson.append("{")
        testStatsJson.append(s"\"testName\":\"$testName\",")
        testStatsJson.append(s"\"status\":\"${stats.getOrElse("status", "进行中")}\",")
        testStatsJson.append(s"\"startTime\":${stats.getOrElse("startTime", 0)},")
        testStatsJson.append(s"\"endTime\":${stats.getOrElse("endTime", 0)},")
        testStatsJson.append(s"\"totalRequests\":${stats.getOrElse("totalRequests", 0)},")
        testStatsJson.append(s"\"successRate\":${stats.getOrElse("overallSuccessRate", 0.0)}")
        testStatsJson.append("}")
        if (i < statsEntries.size - 1) {
          testStatsJson.append(",")
        }
      }
      testStatsJson.append("]")
      
      // 构建性能指标数据
      val perfMetricsJson = new StringBuilder
      perfMetricsJson.append("{")
      val metricEntries = performanceMetrics.toList
      for (i <- metricEntries.indices) {
        val (testName, metrics) = metricEntries(i)
        // 获取最多50个最新的指标数据
        val limitedMetrics = metrics.takeRight(50)
        perfMetricsJson.append(s"\"$testName\":[")
        
        // 将指标数据转换为所需格式
        val formattedMetrics = limitedMetrics.map { case (timestamp, values) =>
          val metricsJson = values.map { case (k, v) => s"\"$k\":$v" }.mkString(",")
          s"{\"timestamp\":$timestamp,\"values\":{$metricsJson}}"
        }
        
        perfMetricsJson.append(formattedMetrics.mkString(","))
        perfMetricsJson.append("]")
        
        if (i < metricEntries.size - 1) {
          perfMetricsJson.append(",")
        }
      }
      perfMetricsJson.append("}")
      
      // 组合最终的JSON
      s"""{
        "testStats": $testStatsJson,
        "performanceMetrics": $perfMetricsJson
      }"""
    }
  }
  
  /**
   * 系统指标API处理器
   */
  private class SystemMetricsApiHandler extends HttpHandler {
    def handle(exchange: HttpExchange): Unit = {
      val response = generateSystemMetricsJson()
      exchange.getResponseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(200, response.length)
      val os = exchange.getResponseBody
      os.write(response.getBytes("UTF-8"))
      os.close()
    }
    
    /**
     * 生成系统指标JSON
     */
    private def generateSystemMetricsJson(): String = {
      val metricsData = systemMetrics.getOrElse("system", ArrayBuffer())
      
      // 获取当前值
      val currentMetrics = if (metricsData.nonEmpty) {
        val (_, metrics) = metricsData.last
        metrics
      } else {
        Map[String, Double]()
      }
      
      // 限制历史数据点数量
      val historyData = metricsData.takeRight(50).map { case (timestamp, metrics) =>
        s"""{"timestamp": $timestamp, "metrics": ${mapToJson(metrics)}}"""
      }.mkString("[", ", ", "]")
      
      s"""{"current": ${mapToJson(currentMetrics)}, "history": $historyData}"""
    }
    
    /**
     * 将Map转换为JSON
     */
    private def mapToJson(map: Map[String, Double]): String = {
      val entries = map.map { case (key, value) =>
        s""""$key": $value"""
      }.mkString(", ")
      s"{$entries}"
    }
  }
  
  /**
   * 性能指标API处理器
   */
  private class PerformanceMetricsApiHandler extends HttpHandler {
    def handle(exchange: HttpExchange): Unit = {
      val response = generatePerformanceMetricsJson()
      exchange.getResponseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(200, response.length)
      val os = exchange.getResponseBody
      os.write(response.getBytes("UTF-8"))
      os.close()
    }
    
    /**
     * 生成性能指标JSON
     */
    private def generatePerformanceMetricsJson(): String = {
      val jsonBuilder = new StringBuilder
      jsonBuilder.append("{")
      
      val entries = performanceMetrics.toList
      for (i <- entries.indices) {
        val (testName, metrics) = entries(i)
        jsonBuilder.append(s""""$testName": [""")
        
        // 限制数据点数量
        val limitedMetrics = metrics.takeRight(50)
        for (j <- limitedMetrics.indices) {
          val (timestamp, metricsMap) = limitedMetrics(j)
          
          jsonBuilder.append(s"""{"timestamp": $timestamp, "metrics": ${mapToJson(metricsMap)}}""")
          if (j < limitedMetrics.size - 1) {
            jsonBuilder.append(", ")
          }
        }
        
        jsonBuilder.append("]")
        if (i < entries.size - 1) {
          jsonBuilder.append(", ")
        }
      }
      
      jsonBuilder.append("}")
      jsonBuilder.toString
    }
    
    /**
     * 将Map转换为JSON
     */
    private def mapToJson(map: Map[String, Double]): String = {
      val entries = map.map { case (key, value) =>
        s""""$key": $value"""
      }.mkString(", ")
      s"{$entries}"
    }
  }
  
  /**
   * 告警API处理器
   */
  private class AlertsApiHandler extends HttpHandler {
    def handle(exchange: HttpExchange): Unit = {
      val response = generateAlertsJson()
      exchange.getResponseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(200, response.length)
      val os = exchange.getResponseBody
      os.write(response.getBytes("UTF-8"))
      os.close()
    }
    
    /**
     * 生成告警JSON
     */
    private def generateAlertsJson(): String = {
      import AlertManager.AlertLevel
      
      val recentAlerts = getRecentAlerts()
      val formattedAlerts = recentAlerts.map { alert =>
        s"""{
          "timestamp": ${alert.timestamp},
          "formattedTime": "${formatTimestamp(alert.timestamp)}",
          "testType": "${alert.testType}",
          "message": "${escapeJsonString(alert.message)}",
          "value": ${alert.value},
          "level": "${alert.level.toString}"
        }"""
      }
      
      // 告警统计
      val alertsByLevel = recentAlerts.groupBy(_.level).map { case (level, alerts) =>
        s""""${level.toString}": ${alerts.size}"""
      }
      
      val alertsByType = recentAlerts.groupBy(_.testType).map { case (testType, alerts) =>
        s""""$testType": ${alerts.size}"""
      }
      
      s"""{
        "alerts": [${formattedAlerts.mkString(",")}],
        "statistics": {
          "byLevel": {${alertsByLevel.mkString(",")}},
          "byType": {${alertsByType.mkString(",")}}
        }
      }"""
    }
    
    /**
     * 格式化时间戳
     */
    private def formatTimestamp(timestamp: Long): String = {
      val dateTime = LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
      )
      dateTime.format(dateTimeFormatter)
    }
    
    /**
     * 转义JSON字符串
     */
    private def escapeJsonString(s: String): String = {
      s.replace("\\", "\\\\")
       .replace("\"", "\\\"")
       .replace("\n", "\\n")
       .replace("\r", "\\r")
       .replace("\t", "\\t")
    }
  }
} 