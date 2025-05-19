package example

import scala.collection.mutable

/**
 * 错误分类器
 * 负责对错误进行分类、统计和分析
 */
object ErrorClassifier {
  // 存储每个测试类型的错误统计
  private val errorStats = mutable.Map[String, mutable.Map[String, Int]]()
  
  // 存储每个测试类型的错误详情
  private val errorDetails = mutable.Map[String, mutable.ArrayBuffer[ErrorDetail]]()
  
  /**
   * 初始化错误分类器
   * @param testType 测试类型
   */
  def initialize(testType: String): Unit = {
    errorStats.put(testType, mutable.Map[String, Int]())
    errorDetails.put(testType, mutable.ArrayBuffer[ErrorDetail]())
  }
  
  /**
   * 分类并记录错误
   * @param testType 测试类型
   * @param errorMessage 错误消息
   * @param responseBody 响应内容
   */
  def classifyError(testType: String, errorMessage: String, responseBody: String): Unit = {
    // 确保测试类型已初始化
    if (!errorStats.contains(testType)) {
      initialize(testType)
    }
    
    // 确定错误类型
    val errorType = determineErrorType(errorMessage, responseBody)
    
    // 更新错误统计
    val stats = errorStats(testType)
    stats.put(errorType, stats.getOrElse(errorType, 0) + 1)
    
    // 记录错误详情
    errorDetails(testType) += ErrorDetail(
      System.currentTimeMillis(),
      errorType,
      errorMessage,
      responseBody
    )
    
    // 限制错误详情数量
    if (errorDetails(testType).size > 1000) {
      errorDetails(testType) = errorDetails(testType).takeRight(1000)
    }
  }
  
  /**
   * 确定错误类型
   * @param errorMessage 错误消息
   * @param responseBody 响应内容
   * @return 错误类型
   */
  private def determineErrorType(errorMessage: String, responseBody: String): String = {
    if (errorMessage.contains("连接超时")) {
      "连接超时"
    } else if (responseBody.contains("云盒USB未插入打印机")) {
      "设备未连接打印机"
    } else if (responseBody.contains("设备已超过有效期")) {
      "设备已过期"
    } else if (responseBody.contains("该设备未联网激活")) {
      "设备未激活"
    } else if (responseBody.contains("设备未归属对应平台")) {
      "设备归属错误"
    } else if (responseBody.contains("网络异常")) {
      "网络异常"
    } else if (responseBody.contains("服务器内部错误") || responseBody.contains("Internal Server Error")) {
      "服务器内部错误"
    } else if (responseBody.contains("参数错误") || responseBody.contains("缺少必要参数")) {
      "参数错误"
    } else if (responseBody.contains("授权失败") || responseBody.contains("unauthorized")) {
      "授权失败"
    } else if (errorMessage.contains("HTTP 40")) {
      "客户端错误"
    } else if (errorMessage.contains("HTTP 50")) {
      "服务器错误"
    } else {
      "其他错误"
    }
  }
  
  /**
   * 获取错误统计
   * @param testType 测试类型
   * @return 错误类型 -> 数量 映射
   */
  def getErrorStats(testType: String): Map[String, Int] = {
    errorStats.getOrElse(testType, mutable.Map()).toMap
  }
  
  /**
   * 获取错误详情
   * @param testType 测试类型
   * @param limit 最大返回数量
   * @return 错误详情列表
   */
  def getErrorDetails(testType: String, limit: Int = 100): List[ErrorDetail] = {
    errorDetails.getOrElse(testType, mutable.ArrayBuffer()).take(limit).toList
  }
  
  /**
   * 获取错误分布（各类型错误占比）
   * @param testType 测试类型
   * @return 错误类型 -> 百分比 映射
   */
  def getErrorDistribution(testType: String): Map[String, Double] = {
    val stats = errorStats.getOrElse(testType, mutable.Map())
    val total = stats.values.sum
    
    if (total == 0) {
      return Map()
    }
    
    stats.map { case (errorType, count) =>
      (errorType, count.toDouble / total * 100)
    }.toMap
  }
  
  /**
   * 获取总错误数
   * @param testType 测试类型
   * @return 错误总数
   */
  def getTotalErrors(testType: String): Int = {
    errorStats.getOrElse(testType, mutable.Map()).values.sum
  }
  
  /**
   * 清除错误数据
   * @param testType 测试类型
   */
  def clearErrors(testType: String): Unit = {
    errorStats.remove(testType)
    errorDetails.remove(testType)
  }
  
  /**
   * 生成错误分析报告
   * @param testType 测试类型
   * @return 错误分析报告
   */
  def generateReport(testType: String): String = {
    // 确保测试类型已初始化
    if (!errorStats.contains(testType)) {
      return "无错误数据"
    }
    
    val stats = errorStats(testType)
    val total = stats.values.sum
    
    if (total == 0) {
      return "无错误"
    }
    
    val sb = new StringBuilder
    sb.append(s"错误分析报告 - $testType\n")
    sb.append("==============================\n\n")
    
    sb.append(s"总错误数: $total\n\n")
    
    sb.append("错误类型分布:\n")
    stats.toList.sortBy(-_._2).foreach { case (errorType, count) =>
      val percentage = count.toDouble / total * 100
      sb.append(f"$errorType: $count ($percentage%.2f%%)\n")
    }
    
    sb.append("\n最近错误示例:\n")
    val recentErrors = errorDetails.getOrElse(testType, mutable.ArrayBuffer()).takeRight(5)
    recentErrors.foreach { error =>
      sb.append(s"[${new java.util.Date(error.timestamp)}] ${error.errorType}: ${error.message}\n")
    }
    
    sb.toString
  }
}

/**
 * 错误详情类
 */
case class ErrorDetail(
  timestamp: Long,
  errorType: String,
  message: String,
  responseBody: String
) 