package example

import java.io.{File, FileWriter, PrintWriter}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.collection.mutable.{Map => MutableMap}

object ResponseLogger {
  private val logDir = "target/gatling/logs"
  private val statsMap = MutableMap[String, MutableMap[String, Int]]()
  
  // 确保日志目录存在
  private def ensureLogDirectory(): Unit = {
    val dir = new File(logDir)
    if (!dir.exists()) {
      dir.mkdirs()
    }
  }

  // 获取当前时间戳
  private def getCurrentTimestamp: String = {
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
  }

  // 初始化测试会话的统计数据
  def initializeStats(testName: String): Unit = {
    statsMap(testName) = MutableMap(
      "success" -> 0,
      "failure" -> 0,
      "timeout" -> 0
    )
  }
  
  // 检查响应是否成功，包含对"云盒USB未插入打印机"的特殊处理
  def checkSuccess(responseBody: String, originalIsSuccess: Boolean): Boolean = {
    if (originalIsSuccess) {
      true
    } else {
      // 额外检查响应中是否包含特殊处理的"云盒USB未插入打印机"
      responseBody.contains("云盒USB未插入打印机") || 
      ResponseChecker.isResponseSuccessful(responseBody)
    }
  }
  
  // 是否为超时错误
  def isTimeout(responseBody: String): Boolean = {
    responseBody.contains("connection timed out") || 
    responseBody.contains("timeout")
  }

  // 记录响应日志
  def logResponse(
    testName: String,
    deviceId: String,
    deviceKey: String,
    statusCode: Int,
    responseBody: String,
    isSuccess: Boolean,
    errorMessage: String = ""
  ): Unit = {
    ensureLogDirectory()
    
    // 检查是否为超时错误
    val isTimeoutError = isTimeout(responseBody) || errorMessage.contains("超时") || statusCode == 0
    
    // 检查是否为真正的成功响应（包括"云盒USB未插入打印机"的特殊情况）
    val actualSuccess = if (isTimeoutError) false else checkSuccess(responseBody, isSuccess)
    
    // 更新统计数据
    val testStats = statsMap.getOrElseUpdate(testName, MutableMap("success" -> 0, "failure" -> 0, "timeout" -> 0))
    if (isTimeoutError) {
      testStats("timeout") += 1
    } else if (actualSuccess) {
      testStats("success") += 1
    } else {
      testStats("failure") += 1
    }

    // 构建日志文件名
    val logFileName = s"$logDir/${testName}_${getCurrentTimestamp}.log"
    
    // 构建日志内容
    val logContent = 
      s"""时间: ${LocalDateTime.now()}
         |设备ID: $deviceId
         |设备密钥: $deviceKey
         |状态码: $statusCode
         |是否成功: $actualSuccess ${if (actualSuccess && !isSuccess) "(云盒USB未插入打印机但视为成功)" else ""} ${if (isTimeoutError) "[连接超时]" else ""}
         |响应内容: $responseBody
         |${if (errorMessage.nonEmpty && (!actualSuccess || isTimeoutError)) s"错误信息: $errorMessage" else ""}
         |当前统计:
         |  - 成功数: ${testStats("success")}
         |  - 失败数: ${testStats("failure")}
         |  - 超时数: ${testStats("timeout")}
         |----------------------------------------
         |""".stripMargin

    // 写入日志文件
    val writer = new PrintWriter(new FileWriter(logFileName, true))
    try {
      writer.write(logContent)
    } finally {
      writer.close()
    }
  }

  // 获取测试统计数据
  def getStats(testName: String): Map[String, Int] = {
    statsMap.getOrElse(testName, MutableMap("success" -> 0, "failure" -> 0, "timeout" -> 0)).toMap
  }

  // 生成测试报告
  def generateReport(testName: String): String = {
    val stats = getStats(testName)
    val totalRequests = stats("success") + stats("failure") + stats("timeout")
    val successRate = if (totalRequests > 0) stats("success").toDouble / totalRequests * 100 else 0.0
    
    s"""
       |测试名称: $testName
       |总请求数: $totalRequests
       |成功请求数: ${stats("success")}
       |失败请求数: ${stats("failure")}
       |超时请求数: ${stats("timeout")}
       |成功率: ${successRate.formatted("%.2f")}%
       |""".stripMargin
  }
} 