package example

import scala.collection.mutable.ArrayBuffer
import java.util.concurrent.{ScheduledExecutorService, Executors, TimeUnit}

/**
 * 动态负载调整器
 * 根据系统响应时间自动调整并发用户数
 */
class DynamicLoadAdjuster(
  val testType: String,
  val initialUsers: Int,
  val maxUsers: Int,
  val stepSize: Int,
  val targetResponseTime: Int,  // 目标响应时间（毫秒）
  val adjustmentIntervalSeconds: Int  // 调整间隔（秒）
) {
  // 当前用户数
  private var currentUsers: Int = initialUsers
  
  // 调整历史记录
  private val adjustmentHistory = ArrayBuffer[(Long, Int, Double)]()
  
  // 调整回调函数：(新用户数, 原因) => Unit
  private var adjustmentCallback: Option[(Int, String) => Unit] = None
  
  // 调度器
  private var scheduler: Option[ScheduledExecutorService] = None
  
  // 是否正在运行
  private var running: Boolean = false
  
  /**
   * 设置调整回调函数
   * @param callback 回调函数，接收新用户数和调整原因
   */
  def setAdjustmentCallback(callback: (Int, String) => Unit): Unit = {
    adjustmentCallback = Some(callback)
  }
  
  /**
   * 启动动态负载调整
   */
  def start(): Unit = {
    if (running) {
      return
    }
    
    running = true
    
    // 创建调度器
    val executor = Executors.newSingleThreadScheduledExecutor()
    scheduler = Some(executor)
    
    // 定期执行调整
    executor.scheduleAtFixedRate(
      new Runnable {
        def run(): Unit = adjustLoad()
      },
      adjustmentIntervalSeconds,
      adjustmentIntervalSeconds,
      TimeUnit.SECONDS
    )
    
    println(s"动态负载调整器已启动。初始用户数: $initialUsers, 目标响应时间: ${targetResponseTime}ms, 调整间隔: ${adjustmentIntervalSeconds}秒")
  }
  
  /**
   * 停止动态负载调整
   */
  def stop(): Unit = {
    if (!running) {
      return
    }
    
    // 关闭调度器
    scheduler.foreach { executor =>
      executor.shutdown()
      try {
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
          executor.shutdownNow()
        }
      } catch {
        case _: InterruptedException => executor.shutdownNow()
      }
    }
    
    running = false
    scheduler = None
    
    println("动态负载调整器已停止")
    
    // 记录调整历史
    recordAdjustmentHistory()
  }
  
  /**
   * 调整负载
   */
  private def adjustLoad(): Unit = {
    try {
      // 获取当前平均响应时间
      val currentAvgResponseTime = getAverageResponseTime()
      if (currentAvgResponseTime <= 0) {
        // 还没有足够的数据
        return
      }
      
      // 计算响应时间比率：当前/目标
      val responseTimeRatio = currentAvgResponseTime.toDouble / targetResponseTime
      
      // 根据响应时间比例决定调整策略
      val newUsers = if (responseTimeRatio > 1.5) {
        // 响应时间远高于目标，大幅减少用户数
        val reduction = (currentUsers * 0.3).toInt.max(stepSize)
        (currentUsers - reduction).max(initialUsers / 2)
      } else if (responseTimeRatio > 1.2) {
        // 响应时间高于目标，小幅减少用户数
        (currentUsers - stepSize).max(initialUsers / 2)
      } else if (responseTimeRatio < 0.5) {
        // 响应时间远低于目标，大幅增加用户数
        val increase = (currentUsers * 0.2).toInt.max(stepSize)
        (currentUsers + increase).min(maxUsers)
      } else if (responseTimeRatio < 0.8) {
        // 响应时间低于目标，小幅增加用户数
        (currentUsers + stepSize).min(maxUsers)
      } else {
        // 响应时间接近目标，保持不变
        currentUsers
      }
      
      // 如果用户数需要调整
      if (newUsers != currentUsers) {
        val reason = if (newUsers > currentUsers) {
          s"响应时间(${currentAvgResponseTime}ms)低于目标(${targetResponseTime}ms)"
        } else {
          s"响应时间(${currentAvgResponseTime}ms)高于目标(${targetResponseTime}ms)"
        }
        
        // 记录调整
        println(s"动态调整用户数: $currentUsers -> $newUsers, 原因: $reason")
        adjustmentHistory += ((System.currentTimeMillis(), newUsers, currentAvgResponseTime))
        
        // 更新当前用户数
        currentUsers = newUsers
        
        // 触发回调
        adjustmentCallback.foreach(_(newUsers, reason))
        
        // 记录到报告
        EnhancedReportGenerator.recordResult(
          testType,
          s"loadAdjustment_${System.currentTimeMillis()}",
          Map(
            "time" -> System.currentTimeMillis(),
            "previousUsers" -> currentUsers,
            "newUsers" -> newUsers,
            "avgResponseTime" -> currentAvgResponseTime,
            "targetResponseTime" -> targetResponseTime,
            "reason" -> reason
          )
        )
      }
    } catch {
      case e: Exception => 
        println(s"动态负载调整出错: ${e.getMessage}")
    }
  }
  
  /**
   * 获取当前平均响应时间
   */
  private def getAverageResponseTime(): Double = {
    // 从ResponseLogger或EnhancedReportGenerator获取最近的平均响应时间
    val recentMetrics = EnhancedReportGenerator.getRecentMetrics(testType, "responseTime", 5)
    if (recentMetrics.isEmpty) 0.0 else recentMetrics.sum / recentMetrics.size
  }
  
  /**
   * 记录调整历史
   */
  private def recordAdjustmentHistory(): Unit = {
    if (adjustmentHistory.isEmpty) {
      return
    }
    
    val history = adjustmentHistory.map { case (timestamp, users, responseTime) =>
      Map(
        "timestamp" -> timestamp,
        "users" -> users,
        "responseTime" -> responseTime
      )
    }.toList
    
    EnhancedReportGenerator.recordResult(testType, "loadAdjustmentHistory", history)
  }
  
  /**
   * 获取当前用户数
   */
  def getCurrentUsers: Int = currentUsers
  
  /**
   * 获取调整历史
   */
  def getAdjustmentHistory: List[(Long, Int, Double)] = adjustmentHistory.toList
} 