package example

import java.lang.management.{ManagementFactory, MemoryMXBean}
import java.time.LocalDateTime
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import scala.collection.mutable.{ArrayBuffer}

/**
 * 系统资源监控工具
 * 用于收集和报告系统资源使用情况，包括CPU、内存和网络
 */
object SystemMonitor {
  private val osBean = ManagementFactory.getOperatingSystemMXBean
    .asInstanceOf[com.sun.management.OperatingSystemMXBean]
  private val memoryBean = ManagementFactory.getMemoryMXBean
  
  // 告警阈值
  private var cpuAlertThreshold = 80.0 // CPU使用率告警阈值，百分比
  private var memoryAlertThreshold = 80.0 // 内存使用率告警阈值，百分比
  private var alertCallbacks = List[(String, Double) => Unit]()
  
  // 记录CPU和内存使用率
  private val cpuUsageHistory = ArrayBuffer[(LocalDateTime, Double)]()
  private val memoryUsageHistory = ArrayBuffer[(LocalDateTime, Long, Long)]()
  
  // 监控调度器
  private var scheduler: Option[ScheduledExecutorService] = None
  private var isMonitoring = false
  private var testName: String = _
  
  /**
   * 启动系统监控
   * @param testNameParam 测试名称
   * @param interval 监控间隔（秒）
   */
  def startMonitoring(testNameParam: String, interval: Int = 5): Unit = {
    if (isMonitoring) {
      println("系统监控已经在运行中")
      return
    }
    
    testName = testNameParam
    
    // 创建和启动调度器
    val executor = Executors.newScheduledThreadPool(1)
    executor.scheduleAtFixedRate(() => collectMetrics(), 0, interval, TimeUnit.SECONDS)
    scheduler = Some(executor)
    isMonitoring = true
    
    println(s"系统监控已启动 - 测试: $testName, 间隔: ${interval}秒")
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
    
    println(s"系统监控已停止 - 测试: $testName")
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
      
      // 收集内存使用情况
      val heapMemoryUsage = memoryBean.getHeapMemoryUsage
      val usedMemory = heapMemoryUsage.getUsed
      val maxMemory = heapMemoryUsage.getMax
      val memoryUsagePercent = (usedMemory.toDouble / maxMemory) * 100
      memoryUsageHistory.append((now, usedMemory, maxMemory))
      
      // 检查是否需要触发告警
      checkAlerts(processCpuLoad, memoryUsagePercent)
    } catch {
      case e: Exception => 
        println(s"收集系统指标时发生错误: ${e.getMessage}")
    }
  }
  
  /**
   * 获取进程CPU使用率
   */
  private def getProcessCpuLoad(): Double = {
    val cpuLoad = osBean.getProcessCpuLoad * 100
    if (cpuLoad < 0) 0.0 else cpuLoad
  }
  
  /**
   * 获取当前CPU使用率
   */
  def getCpuUsage: Double = {
    getProcessCpuLoad()
  }
  
  /**
   * 获取当前内存使用率
   */
  def getMemoryUsage: Double = {
    val heapMemoryUsage = memoryBean.getHeapMemoryUsage
    val usedMemory = heapMemoryUsage.getUsed
    val maxMemory = heapMemoryUsage.getMax
    (usedMemory.toDouble / maxMemory) * 100
  }
  
  /**
   * 设置告警阈值
   */
  def setAlertThresholds(cpu: Double, memory: Double): Unit = {
    cpuAlertThreshold = cpu
    memoryAlertThreshold = memory
    println(s"已设置告警阈值 - CPU: $cpu%, 内存: $memory%")
  }
  
  /**
   * 添加告警回调函数
   */
  def addAlertCallback(callback: (String, Double) => Unit): Unit = {
    alertCallbacks = callback :: alertCallbacks
  }
  
  /**
   * 检查是否需要触发告警
   */
  private def checkAlerts(cpuUsage: Double, memoryUsage: Double): Unit = {
    if (cpuUsage > cpuAlertThreshold) {
      val alertMessage = s"CPU使用率告警: ${cpuUsage.formatted("%.2f")}% > ${cpuAlertThreshold}%"
      AlertManager.addAlert(testName, alertMessage, cpuUsage, AlertManager.AlertLevel.WARNING)
      alertCallbacks.foreach(_(alertMessage, cpuUsage))
    }
    
    if (memoryUsage > memoryAlertThreshold) {
      val alertMessage = s"内存使用率告警: ${memoryUsage.formatted("%.2f")}% > ${memoryAlertThreshold}%"
      AlertManager.addAlert(testName, alertMessage, memoryUsage, AlertManager.AlertLevel.WARNING)
      alertCallbacks.foreach(_(alertMessage, memoryUsage))
    }
  }
} 