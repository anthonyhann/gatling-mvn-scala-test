package example

import java.lang.management.{ManagementFactory, MemoryMXBean, MemoryUsage}
import java.io.{File, FileWriter, PrintWriter}
import java.time.{LocalDateTime, Duration}
import java.time.format.DateTimeFormatter
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import scala.collection.mutable.{Map => MutableMap, ArrayBuffer}
import scala.util.{Try, Success, Failure}
import com.sun.management.OperatingSystemMXBean

/**
 * 系统资源监控工具
 * 用于收集和报告系统资源使用情况，包括CPU、内存和网络
 */
object SystemMonitor {
  // ANSI颜色代码常量
  private val ANSI_RESET = "\u001B[0m"
  private val ANSI_RED = "\u001B[31m"
  private val ANSI_GREEN = "\u001B[32m"
  private val ANSI_YELLOW = "\u001B[33m"
  
  private val monitorDir = "target/gatling/system-monitor"
  private val runtimeBean = ManagementFactory.getRuntimeMXBean
  private val osBean = ManagementFactory.getOperatingSystemMXBean.asInstanceOf[com.sun.management.OperatingSystemMXBean]
  private val memoryBean = ManagementFactory.getMemoryMXBean
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  
  // 存储监控数据
  private val cpuUsageHistory = ArrayBuffer[(LocalDateTime, Double)]()
  private val memoryUsageHistory = ArrayBuffer[(LocalDateTime, Long, Long)]() // 时间, 已用内存, 总内存
  private val testMetrics = MutableMap[String, MutableMap[String, ArrayBuffer[Double]]]()
  
  // 告警阈值
  private var cpuAlertThreshold = 80.0 // CPU使用率告警阈值，百分比
  private var memoryAlertThreshold = 80.0 // 内存使用率告警阈值，百分比
  private var alertCallbacks = List[(String, Double) => Unit]()
  
  // 监控资源类型
  private val resourceMonitors = MutableMap[String, Boolean]()
  
  // 监控调度器
  private var scheduler: Option[ScheduledExecutorService] = None
  private var isMonitoring = false
  private var startTime: LocalDateTime = _
  private var testName: String = _

  /**
   * 启动系统监控
   * @param testNameParam 测试名称
   * @param interval 监控间隔（秒）
   */
  def startMonitoring(testNameParam: String, interval: Int = 5): Unit = {
    if (isMonitoring) {
      println(s"${ANSI_YELLOW}系统监控已经在运行中${ANSI_RESET}")
      return
    }
    
    testName = testNameParam
    startTime = LocalDateTime.now()
    ensureMonitorDirectory()
    
    // 初始化测试指标存储
    testMetrics.getOrElseUpdate(testName, MutableMap(
      "cpuUsage" -> ArrayBuffer[Double](),
      "memoryUsage" -> ArrayBuffer[Double](),
      "gcTime" -> ArrayBuffer[Double]()
    ))
    
    // 创建和启动调度器
    val executor = Executors.newScheduledThreadPool(1)
    executor.scheduleAtFixedRate(() => collectMetrics(), 0, interval, TimeUnit.SECONDS)
    scheduler = Some(executor)
    isMonitoring = true
    
    println(s"${ANSI_GREEN}系统监控已启动 - 测试: $testName, 间隔: ${interval}秒${ANSI_RESET}")
    
    // 记录监控开始日志
    logEvent(s"开始监控系统资源 - 测试: $testName")
  }
  
  /**
   * 停止系统监控
   */
  def stopMonitoring(): Unit = {
    scheduler.foreach { executor =>
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
    
    isMonitoring = false
    scheduler = None
    
    // 生成监控报告
    generateMonitoringReport()
    
    println(s"${ANSI_GREEN}系统监控已停止 - 测试: $testName${ANSI_RESET}")
    logEvent(s"停止监控系统资源 - 测试: $testName")
  }
  
  /**
   * 添加资源监控器
   * @param resourceType 资源类型：cpu, memory, network, disk等
   */
  def addResourceMonitor(resourceType: String): Unit = {
    resourceMonitors.put(resourceType.toLowerCase, true)
  }
  
  /**
   * 获取当前CPU使用率
   * @return CPU使用率百分比
   */
  def getCpuUsage: Double = {
    if (cpuUsageHistory.isEmpty) {
      getProcessCpuLoad()
    } else {
      cpuUsageHistory.last._2
    }
  }
  
  /**
   * 获取当前内存使用率
   * @return 内存使用率百分比
   */
  def getMemoryUsage: Double = {
    if (memoryUsageHistory.isEmpty) {
      val heapMemoryUsage = memoryBean.getHeapMemoryUsage
      val usedMemory = heapMemoryUsage.getUsed
      val maxMemory = heapMemoryUsage.getMax
      (usedMemory.toDouble / maxMemory) * 100
    } else {
      val (_, used, max) = memoryUsageHistory.last
      (used.toDouble / max) * 100
    }
  }
  
  /**
   * 获取监控数据用于报告
   * @param testNameParam 测试名称
   * @return 监控数据Map
   */
  def getMonitoringDataForReport(testNameParam: String): Map[String, Any] = {
    val result = MutableMap[String, Any]()
    
    // CPU数据
    val cpuData = cpuUsageHistory.map { case (time, value) =>
      Map(
        "time" -> time.format(dateTimeFormatter),
        "value" -> value
      )
    }.toList
    result("cpu") = cpuData
    
    // 内存数据
    val memoryData = memoryUsageHistory.map { case (time, used, max) =>
      Map(
        "time" -> time.format(dateTimeFormatter),
        "value" -> (used.toDouble / max) * 100,
        "usedMB" -> used / (1024 * 1024),
        "maxMB" -> max / (1024 * 1024)
      )
    }.toList
    result("memory") = memoryData
    
    // 其他监控数据
    resourceMonitors.keys.foreach { resourceType =>
      if (!result.contains(resourceType)) {
        result(resourceType) = List[Map[String, Any]]()
      }
    }
    
    result.toMap
  }
  
  /**
   * 收集系统指标
   */
  private def collectMetrics(): Unit = {
    try {
      val now = LocalDateTime.now()
      
      // 收集CPU使用率
      val processCpuLoad = getProcessCpuLoad()
      cpuUsageHistory.append((now, processCpuLoad))
      testMetrics(testName)("cpuUsage").append(processCpuLoad)
      
      // 收集内存使用情况
      val heapMemoryUsage = memoryBean.getHeapMemoryUsage
      val usedMemory = heapMemoryUsage.getUsed
      val maxMemory = heapMemoryUsage.getMax
      val memoryUsagePercent = (usedMemory.toDouble / maxMemory) * 100
      memoryUsageHistory.append((now, usedMemory, maxMemory))
      testMetrics(testName)("memoryUsage").append(memoryUsagePercent)
      
      // 检查是否需要触发告警
      checkAlerts(processCpuLoad, memoryUsagePercent)
      
      // 每10次收集写入一次日志
      if (cpuUsageHistory.size % 10 == 0) {
        logMetrics()
      }
    } catch {
      case e: Exception => 
        println(s"${ANSI_RED}收集系统指标时发生错误: ${e.getMessage}${ANSI_RESET}")
    }
  }
  
  /**
   * 获取进程CPU使用率
   */
  private def getProcessCpuLoad(): Double = {
    val cpuLoad = osBean match {
      case bean: com.sun.management.OperatingSystemMXBean =>
        bean.getProcessCpuLoad * 100
      case _ =>
        // 如果无法直接获取，使用系统平均负载作为替代
        osBean.getSystemLoadAverage
    }
    if (cpuLoad < 0) 0.0 else cpuLoad
  }
  
  /**
   * 检查是否需要触发告警
   */
  private def checkAlerts(cpuUsage: Double, memoryUsage: Double): Unit = {
    if (cpuUsage > cpuAlertThreshold) {
      val alertMessage = s"CPU使用率告警: ${cpuUsage.formatted("%.2f")}% > ${cpuAlertThreshold}%"
      logAlert(alertMessage)
      alertCallbacks.foreach(_(alertMessage, cpuUsage))
    }
    
    if (memoryUsage > memoryAlertThreshold) {
      val alertMessage = s"内存使用率告警: ${memoryUsage.formatted("%.2f")}% > ${memoryAlertThreshold}%"
      logAlert(alertMessage)
      alertCallbacks.foreach(_(alertMessage, memoryUsage))
    }
  }
  
  /**
   * 设置告警阈值
   */
  def setAlertThresholds(cpu: Double, memory: Double): Unit = {
    cpuAlertThreshold = cpu
    memoryAlertThreshold = memory
    println(s"${ANSI_YELLOW}已设置告警阈值 - CPU: $cpu%, 内存: $memory%${ANSI_RESET}")
  }
  
  /**
   * 添加告警回调函数
   */
  def addAlertCallback(callback: (String, Double) => Unit): Unit = {
    alertCallbacks = callback :: alertCallbacks
  }
  
  /**
   * 记录指标到日志文件
   */
  private def logMetrics(): Unit = {
    val now = LocalDateTime.now()
    val logFileName = s"$monitorDir/${testName}_system_metrics.csv"
    
    // 确保文件存在并有标题行
    ensureMetricsFileWithHeader(logFileName)
    
    // 获取最新的指标
    val cpuUsage = if (cpuUsageHistory.nonEmpty) 
                    cpuUsageHistory.last._2.formatted("%.2f") else "0.0"
    val memUsage = if (memoryUsageHistory.nonEmpty) 
                    (memoryUsageHistory.last._2.toDouble / memoryUsageHistory.last._3 * 100).formatted("%.2f") else "0.0"
    val memUsedMB = if (memoryUsageHistory.nonEmpty) 
                    (memoryUsageHistory.last._2 / (1024 * 1024)).toString else "0"
    val memTotalMB = if (memoryUsageHistory.nonEmpty) 
                     (memoryUsageHistory.last._3 / (1024 * 1024)).toString else "0"
    
    // 写入日志
    Try {
      val writer = new PrintWriter(new FileWriter(logFileName, true))
      try {
        writer.println(s"${now.format(dateTimeFormatter)},$cpuUsage,$memUsage,$memUsedMB,$memTotalMB")
      } finally {
        writer.close()
      }
    } match {
      case Success(_) => // 成功写入
      case Failure(e) => println(s"${ANSI_RED}写入指标日志失败: ${e.getMessage}${ANSI_RESET}")
    }
  }
  
  /**
   * 确保指标文件存在并有标题行
   */
  private def ensureMetricsFileWithHeader(fileName: String): Unit = {
    val file = new File(fileName)
    if (!file.exists()) {
      Try {
        val writer = new PrintWriter(new FileWriter(file))
        try {
          writer.println("时间戳,CPU使用率(%),内存使用率(%),已用内存(MB),总内存(MB)")
        } finally {
          writer.close()
        }
      } match {
        case Success(_) => // 成功创建
        case Failure(e) => println(s"${ANSI_RED}创建指标文件失败: ${e.getMessage}${ANSI_RESET}")
      }
    }
  }
  
  /**
   * 记录告警信息
   */
  private def logAlert(alertMessage: String): Unit = {
    val logFileName = s"$monitorDir/${testName}_alerts.log"
    val now = LocalDateTime.now()
    
    Try {
      val writer = new PrintWriter(new FileWriter(logFileName, true))
      try {
        writer.println(s"[${now.format(dateTimeFormatter)}] $alertMessage")
      } finally {
        writer.close()
      }
    } match {
      case Success(_) => 
        println(s"${ANSI_RED}告警: $alertMessage${ANSI_RESET}")
      case Failure(e) => 
        println(s"${ANSI_RED}记录告警失败: ${e.getMessage}${ANSI_RESET}")
    }
  }
  
  /**
   * 记录事件信息
   */
  private def logEvent(eventMessage: String): Unit = {
    val logFileName = s"$monitorDir/${testName}_events.log"
    val now = LocalDateTime.now()
    
    Try {
      val writer = new PrintWriter(new FileWriter(logFileName, true))
      try {
        writer.println(s"[${now.format(dateTimeFormatter)}] $eventMessage")
      } finally {
        writer.close()
      }
    } match {
      case Success(_) => // 成功记录
      case Failure(e) => println(s"${ANSI_RED}记录事件失败: ${e.getMessage}${ANSI_RESET}")
    }
  }
  
  /**
   * 确保监控目录存在
   */
  private def ensureMonitorDirectory(): Unit = {
    val dir = new File(monitorDir)
    if (!dir.exists()) {
      dir.mkdirs()
    }
  }
  
  /**
   * 生成监控报告
   */
  private def generateMonitoringReport(): Unit = {
    if (cpuUsageHistory.isEmpty || memoryUsageHistory.isEmpty) {
      println(s"${ANSI_YELLOW}没有足够的监控数据生成报告${ANSI_RESET}")
      return
    }
    
    val reportFileName = s"$monitorDir/${testName}_monitoring_report.txt"
    val now = LocalDateTime.now()
    val duration = Duration.between(startTime, now)
    
    Try {
      val writer = new PrintWriter(new FileWriter(reportFileName))
      try {
        writer.println("=============================================")
        writer.println(s"系统资源监控报告 - ${testName}")
        writer.println("=============================================")
        writer.println(s"开始时间: ${startTime.format(dateTimeFormatter)}")
        writer.println(s"结束时间: ${now.format(dateTimeFormatter)}")
        writer.println(s"监控持续时间: ${formatDuration(duration)}")
        writer.println("---------------------------------------------")
        
        // CPU使用率统计
        val cpuValues = cpuUsageHistory.map(_._2)
        val avgCpu = cpuValues.sum / cpuValues.size
        val maxCpu = cpuValues.max
        val minCpu = cpuValues.min
        
        writer.println("CPU使用率统计:")
        writer.println(s"  - 平均: ${avgCpu.formatted("%.2f")}%")
        writer.println(s"  - 最大: ${maxCpu.formatted("%.2f")}%")
        writer.println(s"  - 最小: ${minCpu.formatted("%.2f")}%")
        writer.println("---------------------------------------------")
        
        // 内存使用率统计
        val memoryPercentages = memoryUsageHistory.map(m => (m._2.toDouble / m._3) * 100)
        val avgMemory = memoryPercentages.sum / memoryPercentages.size
        val maxMemory = memoryPercentages.max
        val minMemory = memoryPercentages.min
        val lastMemoryUsed = memoryUsageHistory.last._2 / (1024 * 1024)
        val lastMemoryTotal = memoryUsageHistory.last._3 / (1024 * 1024)
        
        writer.println("内存使用率统计:")
        writer.println(s"  - 平均: ${avgMemory.formatted("%.2f")}%")
        writer.println(s"  - 最大: ${maxMemory.formatted("%.2f")}%")
        writer.println(s"  - 最小: ${minMemory.formatted("%.2f")}%")
        writer.println(s"  - 当前使用: ${lastMemoryUsed}MB / ${lastMemoryTotal}MB")
        writer.println("=============================================")
      } finally {
        writer.close()
      }
      
      println(s"${ANSI_GREEN}系统监控报告已生成: $reportFileName${ANSI_RESET}")
      
    } match {
      case Success(_) => // 成功生成报告
      case Failure(e) => println(s"${ANSI_RED}生成监控报告失败: ${e.getMessage}${ANSI_RESET}")
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
} 