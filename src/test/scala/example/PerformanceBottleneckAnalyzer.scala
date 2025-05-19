package example

import scala.collection.mutable

/**
 * 性能瓶颈分析器
 * 负责分析性能测试中的瓶颈并生成报告
 */
object PerformanceBottleneckAnalyzer {
  
  // 存储性能指标
  private val metricsHistory = mutable.ArrayBuffer[Map[String, Double]]()
  
  // 瓶颈分析结果
  private val bottlenecks = mutable.Map[String, BottleneckInfo]()
  
  /**
   * 添加指标数据点
   * @param metrics 指标数据
   */
  def addMetricsDataPoint(metrics: Map[String, Double]): Unit = {
    synchronized {
      metricsHistory.append(metrics)
      
      // 限制历史数据量
      if (metricsHistory.size > 1000) {
        metricsHistory.remove(0, metricsHistory.size - 1000)
      }
    }
  }
  
  /**
   * 分析瓶颈
   * @return 瓶颈分析结果
   */
  def analyzeBottlenecks(): BottleneckAnalysisResult = {
    synchronized {
      // 清空当前结果
      bottlenecks.clear()
      
      // 如果没有足够的数据进行分析
      if (metricsHistory.size < 10) {
        return new BottleneckAnalysisResult(Map(), "数据点不足，无法进行瓶颈分析")
      }
      
      // 分析响应时间趋势
      analyzeResponseTimeTrend()
      
      // 分析错误率趋势
      analyzeErrorRateTrend()
      
      // 分析吞吐量趋势
      analyzeThroughputTrend()
      
      // 创建分析结果
      new BottleneckAnalysisResult(bottlenecks.toMap, generateSummary())
    }
  }
  
  /**
   * 分析响应时间趋势
   */
  private def analyzeResponseTimeTrend(): Unit = {
    val responseTimePoints = metricsHistory.flatMap(_.get("responseTime"))
    
    if (responseTimePoints.size < 10) {
      return
    }
    
    // 获取最近的样本
    val recentPoints = responseTimePoints.takeRight(10)
    val avgResponseTime = recentPoints.sum / recentPoints.size
    
    // 计算趋势
    val firstHalf = recentPoints.take(5)
    val secondHalf = recentPoints.takeRight(5)
    val firstHalfAvg = firstHalf.sum / firstHalf.size
    val secondHalfAvg = secondHalf.sum / secondHalf.size
    val trend = secondHalfAvg - firstHalfAvg
    
    // 判断是否存在瓶颈
    val isBottleneck = avgResponseTime > 2000 || (avgResponseTime > 1000 && trend > 100)
    
    // 添加到瓶颈列表
    if (isBottleneck) {
      bottlenecks("responseTime") = BottleneckInfo(
        "响应时间",
        avgResponseTime,
        trend,
        s"平均响应时间为 ${avgResponseTime.toInt} ms，趋势为 ${trend.toInt} ms，可能存在性能瓶颈",
        if (trend > 0) "上升" else if (trend < 0) "下降" else "稳定",
        List(
          "检查数据库查询性能",
          "检查外部API调用",
          "检查服务器资源使用情况",
          "考虑优化代码或增加缓存"
        )
      )
    }
  }
  
  /**
   * 分析错误率趋势
   */
  private def analyzeErrorRateTrend(): Unit = {
    val errorRatePoints = metricsHistory.flatMap(m => 
      if (m.contains("successRate")) Some(100 - m("successRate")) else None
    )
    
    if (errorRatePoints.size < 10) {
      return
    }
    
    // 获取最近的样本
    val recentPoints = errorRatePoints.takeRight(10)
    val avgErrorRate = recentPoints.sum / recentPoints.size
    
    // 计算趋势
    val firstHalf = recentPoints.take(5)
    val secondHalf = recentPoints.takeRight(5)
    val firstHalfAvg = firstHalf.sum / firstHalf.size
    val secondHalfAvg = secondHalf.sum / secondHalf.size
    val trend = secondHalfAvg - firstHalfAvg
    
    // 判断是否存在瓶颈
    val isBottleneck = avgErrorRate > 5 || (avgErrorRate > 1 && trend > 1)
    
    // 添加到瓶颈列表
    if (isBottleneck) {
      bottlenecks("errorRate") = BottleneckInfo(
        "错误率",
        avgErrorRate,
        trend,
        s"平均错误率为 ${avgErrorRate.toInt}%，趋势为 ${trend.toInt}%，需要关注",
        if (trend > 0) "上升" else if (trend < 0) "下降" else "稳定",
        List(
          "检查错误日志",
          "检查网络连接",
          "检查API响应状态",
          "检查服务是否过载"
        )
      )
    }
  }
  
  /**
   * 分析吞吐量趋势
   */
  private def analyzeThroughputTrend(): Unit = {
    // 这里假设有吞吐量指标，如果没有，可以从响应时间和并发用户数推算
    val tpsPoints = metricsHistory.flatMap(_.get("tps"))
    
    if (tpsPoints.size < 10) {
      return
    }
    
    // 获取最近的样本
    val recentPoints = tpsPoints.takeRight(10)
    val avgTps = recentPoints.sum / recentPoints.size
    
    // 计算趋势
    val firstHalf = recentPoints.take(5)
    val secondHalf = recentPoints.takeRight(5)
    val firstHalfAvg = firstHalf.sum / firstHalf.size
    val secondHalfAvg = secondHalf.sum / secondHalf.size
    val trend = secondHalfAvg - firstHalfAvg
    
    // 判断是否存在瓶颈
    val isBottleneck = (avgTps < 50 && trend < 0) || (avgTps < 20)
    
    // 添加到瓶颈列表
    if (isBottleneck) {
      bottlenecks("throughput") = BottleneckInfo(
        "吞吐量",
        avgTps,
        trend,
        s"平均TPS为 ${avgTps.toInt}，趋势为 ${trend.toInt}，系统吞吐能力存在问题",
        if (trend > 0) "上升" else if (trend < 0) "下降" else "稳定",
        List(
          "检查系统资源使用情况",
          "检查数据库连接池",
          "检查网络带宽",
          "考虑增加服务器资源"
        )
      )
    }
  }
  
  /**
   * 生成分析总结
   * @return 分析总结
   */
  private def generateSummary(): String = {
    if (bottlenecks.isEmpty) {
      return "系统性能良好，未发现明显瓶颈。"
    }
    
    val sb = new StringBuilder
    sb.append("性能瓶颈分析结果:\n\n")
    
    // 添加瓶颈信息
    bottlenecks.values.foreach { info =>
      sb.append(s"问题: ${info.name}\n")
      sb.append(s"当前值: ${info.value.toInt}\n")
      sb.append(s"趋势: ${info.trend.toInt} (${info.trendDescription})\n")
      sb.append(s"描述: ${info.description}\n")
      sb.append("建议:\n")
      info.recommendations.foreach { rec =>
        sb.append(s"- $rec\n")
      }
      sb.append("\n")
    }
    
    // 添加总结
    if (bottlenecks.contains("responseTime") && bottlenecks.contains("throughput")) {
      sb.append("系统存在严重性能问题，响应时间和吞吐量同时出现异常，建议立即检查系统资源和代码瓶颈。")
    } else if (bottlenecks.contains("responseTime")) {
      sb.append("系统响应时间异常，可能是处理逻辑、数据库查询或外部调用导致的延迟。")
    } else if (bottlenecks.contains("throughput")) {
      sb.append("系统吞吐量异常，可能是资源限制或并发处理能力不足。")
    } else if (bottlenecks.contains("errorRate")) {
      sb.append("系统错误率异常，需检查日志确定错误原因。")
    }
    
    sb.toString
  }
  
  /**
   * 清空历史数据
   */
  def clear(): Unit = {
    synchronized {
      metricsHistory.clear()
      bottlenecks.clear()
    }
  }
}

/**
 * 瓶颈信息
 */
case class BottleneckInfo(
  name: String,
  value: Double,
  trend: Double,
  description: String,
  trendDescription: String,
  recommendations: List[String]
)

/**
 * 瓶颈分析结果
 */
class BottleneckAnalysisResult(
  val bottlenecks: Map[String, BottleneckInfo],
  val summary: String
) {
  /**
   * 生成报告
   * @return 报告文本
   */
  def generateReport(): String = summary
  
  /**
   * 是否存在瓶颈
   * @return 存在瓶颈返回true
   */
  def hasBottlenecks: Boolean = bottlenecks.nonEmpty
} 