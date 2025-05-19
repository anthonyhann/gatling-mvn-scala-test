package example

/**
 * 告警数据类
 * 表示系统告警
 */
case class Alert(
  timestamp: Long,
  testType: String,
  message: String,
  value: Double,
  level: AlertManager.AlertLevel.Value
) 