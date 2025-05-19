package example

import java.time.LocalDateTime
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
  private val metricsData = new TrieMap[String, ArrayBuffer[Map[String, Double]]]()
  
  // 活跃测试集合
  private val activeTests = MutableSet[String]()
  
  /**
   * 初始化报告
   * @param testType 测试类型
   * @param title 测试标题
   * @param description 测试描述
   */
  def initializeReport(testType: String, title: String, description: String): Unit = {
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
    metricsData.getOrElseUpdate(testType, ArrayBuffer[Map[String, Double]]())
    
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
    val metricsArray = metricsData.getOrElseUpdate(testType, ArrayBuffer[Map[String, Double]]())
    
    // 添加新指标
    metricsArray.synchronized {
      metricsArray += metrics
      
      // 限制数据点数量，保持最新的2000个点
      if (metricsArray.size > 2000) {
        metricsData(testType) = metricsArray.takeRight(2000)
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
    val metricsArray = metricsData.getOrElse(testType, ArrayBuffer[Map[String, Double]]())
    if (metricsArray.isEmpty) Map.empty[String, Double]
    else metricsArray.last
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
    
    // 从活跃测试中移除
    activeTests -= testType
    
    println(s"完成测试报告: $testType, 状态: ${testResultMap("status")}")
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
} 