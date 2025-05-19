package example

import java.time.LocalDateTime

/**
 * 告警管理器
 * 管理系统告警信息
 */
object AlertManager {
  /**
   * 告警级别枚举
   */
  object AlertLevel extends Enumeration {
    val INFO, WARNING, ERROR, CRITICAL = Value
  }
  
  // 告警历史记录
  private var alerts = List[Alert]()
  
  /**
   * 添加告警
   */
  def addAlert(testType: String, message: String, value: Double, level: AlertLevel.Value): Alert = {
    val alert = Alert(
      timestamp = System.currentTimeMillis(),
      testType = testType,
      message = message,
      value = value,
      level = level
    )
    
    // 添加到历史记录
    synchronized {
      alerts = alert :: alerts
      // 只保留最近的100条告警
      if (alerts.size > 100) {
        alerts = alerts.take(100)
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
  def getRecentAlerts(limit: Int = 20): List[Alert] = {
    synchronized {
      alerts.sortBy(-_.timestamp).take(limit)
    }
  }
  
  /**
   * 获取指定测试的告警
   */
  def getAlertsForTest(testType: String): List[Alert] = {
    synchronized {
      alerts.filter(_.testType == testType)
    }
  }
  
  /**
   * 清除告警历史
   */
  def clearAlerts(): Unit = {
    synchronized {
      alerts = List.empty
    }
  }
} 