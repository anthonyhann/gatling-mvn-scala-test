package example

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object ResponseChecker {
  // 检查响应状态和内容
  val successfulResponse = 
    status.is(200)
      .saveAs("httpStatus")

  // 保存响应信息
  val saveResponseInfo = List(
    responseTimeInMillis.saveAs("responseTime"),
    bodyString.saveAs("responseBody"),
    jsonPath("$.code").is("200").saveAs("responseCode"),
    jsonPath("$.msg").is("success").saveAs("responseMsg"),
    jsonPath("$.data.task_id").exists.saveAs("taskId"),
    jsonPath("$.data.task_priority").exists.saveAs("taskPriority")
  )

  // 判断是否为连接超时错误
  def isConnectionTimeout(responseBody: String): Boolean = {
    responseBody.contains("connection timed out") || 
    responseBody.contains("timeout") ||
    responseBody == ""
  }

  // 判断响应是否成功
  def isResponseSuccessful(responseBody: String): Boolean = {
    try {
      // 直接使用正则表达式解析响应
      val codePattern = """"code":\s*"?(\d+)"?""".r
      val msgPattern = """"msg":\s*"([^"]*)"?""".r
      
      // 提取code和msg
      val codeOpt = codePattern.findFirstMatchIn(responseBody).map(_.group(1))
      val msgOpt = msgPattern.findFirstMatchIn(responseBody).map(_.group(1))
      
      // 检查是否成功（code为200或特定的错误码）
      val isSuccessCode = codeOpt.exists(code => 
        code == "200" || 
        code == "5051" || // USB未插入打印机
        code == "10009" || // 设备已超期
        code == "10011" || // 设备未激活  
        code == "11013" || // 设备未在线
        code == "11404" || // 设备不存在
        code == "10015"    // 预览失败
      )
      
      // 检查特殊消息（即使code不是成功码，但这些消息是可接受的）
      val isAcceptableMessage = msgOpt.exists(msg => 
        msg == "success" ||
        msg.contains("云盒USB未插入打印机") || 
        msg.contains("设备已超过有效期") || 
        msg.contains("该设备未联网激活") ||
        msg.contains("设备未归属对应平台")
      )
      
      isSuccessCode || isAcceptableMessage
    } catch {
      case e: Exception => 
        println(s"解析响应时出错: ${e.getMessage}, 响应体: ${responseBody.take(100)}...")
        false
    }
  }

  // 提取任务ID和优先级
  def extractTaskInfo(responseBody: String): (String, String) = {
    try {
      val taskIdPattern = """"task_id":\s*"([^"]+)"""".r
      val priorityPattern = """"task_priority":\s*(\d+)""".r
      
      val taskId = taskIdPattern.findFirstMatchIn(responseBody).map(_.group(1)).getOrElse("")
      val priority = priorityPattern.findFirstMatchIn(responseBody).map(_.group(1)).getOrElse("0")
      
      (taskId, priority)
    } catch {
      case e: Exception => 
        println(s"提取任务信息时出错: ${e.getMessage}, 响应体: ${responseBody.take(100)}...")
        ("", "0")
    }
  }
} 