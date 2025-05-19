package example

import java.time.LocalDateTime
import scala.collection.mutable

/**
 * 告警管理器
 * 管理系统告警信息和阈值
 */
object AlertManager {
  /**
   * 告警级别枚举
   */
  object AlertLevel extends Enumeration {
    val INFO, WARNING, ERROR, CRITICAL = Value
  }
  
  // 告警历史记录
  private val alerts = mutable.Map[String, mutable.ListBuffer[Alert]]()
  
  // 告警阈值配置
  private val thresholds = mutable.Map[String, AlertThresholds]()
  
  /**
   * 初始化告警管理器
   * @param testType 测试类型
   */
  def initialize(testType: String): Unit = {
    if (!alerts.contains(testType)) {
      alerts(testType) = mutable.ListBuffer[Alert]()
    }
    
    // 设置默认阈值
    if (!thresholds.contains(testType)) {
      thresholds(testType) = AlertThresholds(70.0, 80.0, 5.0, 3000)
    }
    
    println(s"告警管理器已初始化: $testType")
  }
  
  /**
   * 设置告警阈值
   */
  def setAlertThresholds(
    testType: String,
    cpuThreshold: Double,
    memoryThreshold: Double,
    errorRateThreshold: Double,
    responseTimeThreshold: Int
  ): Unit = {
    thresholds(testType) = AlertThresholds(
      cpuThreshold,
      memoryThreshold,
      errorRateThreshold,
      responseTimeThreshold
    )
    
    println(s"已设置告警阈值 - 测试: $testType, CPU: $cpuThreshold%, 内存: $memoryThreshold%, 错误率: $errorRateThreshold%, 响应时间: ${responseTimeThreshold}ms")
  }
  
  /**
   * 发送告警
   */
  def sendAlert(testType: String, message: String, value: Double, level: AlertLevel.Value = AlertLevel.WARNING): Alert = {
    val alert = Alert(
      timestamp = System.currentTimeMillis(),
      testType = testType,
      message = message,
      value = value,
      level = level
    )
    
    // 添加到历史记录
    synchronized {
      if (!alerts.contains(testType)) {
        initialize(testType)
      }
      
      alerts(testType) += alert
      
      // 只保留最近的100条告警
      if (alerts(testType).size > 100) {
        alerts(testType) = alerts(testType).takeRight(100)
      }
    }
    
    // 添加到实时监控
    if (RealTimeMonitorServer.isRunning) {
      RealTimeMonitorServer.addAlert(alert)
    }
    
    // 输出告警信息
    val levelPrefix = level match {
      case AlertLevel.INFO => "[INFO]"
      case AlertLevel.WARNING => "[WARNING]"
      case AlertLevel.ERROR => "[ERROR]"
      case AlertLevel.CRITICAL => "[CRITICAL]"
    }
    
    println(s"$levelPrefix $testType: $message (${value.formatted("%.2f")})")
    
    alert
  }
  
  /**
   * 获取最近的告警
   */
  def getRecentAlerts(testType: String, limit: Int = 20): List[Alert] = {
    synchronized {
      if (!alerts.contains(testType)) {
        return List.empty
      }
      alerts(testType).toList.sortBy(-_.timestamp).take(limit)
    }
  }
  
  /**
   * 清除告警历史
   */
  def clearAlerts(testType: String): Unit = {
    synchronized {
      alerts.remove(testType)
    }
  }
}

/**
 * 告警阈值配置
 */
case class AlertThresholds(
  cpuThreshold: Double,
  memoryThreshold: Double,
  errorRateThreshold: Double,
  responseTimeThreshold: Int
) 