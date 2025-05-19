package example

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import play.api.libs.json._

/**
 * 响应检查器
 * 负责验证请求响应是否成功
 */
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
      // 使用Play JSON解析响应
      val json = Json.parse(responseBody)
      
      // 提取code和msg
      val code = (json \ "code").asOpt[String].getOrElse("")
      val msg = (json \ "msg").asOpt[String].getOrElse("")
      
      // 检查是否成功（code为200或特定的错误码）
      val isSuccessCode = Seq("200", "5051", "10009", "10011", "11013", "11404", "10015").contains(code)
      
      // 检查特殊消息（即使code不是成功码，但这些消息是可接受的）
      val isAcceptableMessage = 
        msg == "success" ||
        msg.contains("云盒USB未插入打印机") || 
        msg.contains("设备已超过有效期") || 
        msg.contains("该设备未联网激活") ||
        msg.contains("设备未归属对应平台")
      
      isSuccessCode || isAcceptableMessage
    } catch {
      case e: Exception => 
        println(s"解析响应时出错: ${e.getMessage}, 响应体: ${responseBody.take(100)}...")
        // 尝试使用正则表达式作为备选方案
        fallbackResponseCheck(responseBody)
    }
  }
  
  // 使用正则表达式作为备选方案处理响应
  private def fallbackResponseCheck(responseBody: String): Boolean = {
    try {
      // 直接使用正则表达式解析响应
      val codePattern = """"code":\s*"?(\d+)"?""".r
      val msgPattern = """"msg":\s*"([^"]*)"?""".r
      
      // 提取code和msg
      val codeOpt = codePattern.findFirstMatchIn(responseBody).map(_.group(1))
      val msgOpt = msgPattern.findFirstMatchIn(responseBody).map(_.group(1))
      
      // 检查是否成功（code为200或特定的错误码）
      val isSuccessCode = codeOpt.exists(code => 
        Seq("200", "5051", "10009", "10011", "11013", "11404", "10015").contains(code)
      )
      
      // 检查特殊消息
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
        println(s"备选方案解析响应时出错: ${e.getMessage}, 响应体: ${responseBody.take(100)}...")
        false
    }
  }

  // 提取任务ID和优先级
  def extractTaskInfo(responseBody: String): (String, String) = {
    try {
      // 使用Play JSON解析响应
      val json = Json.parse(responseBody)
      
      // 提取task_id和task_priority，如果不存在则使用默认值
      val taskId = (json \ "data" \ "task_id").asOpt[String].getOrElse("")
      val priority = (json \ "data" \ "task_priority").asOpt[Int].getOrElse(0).toString
      
      (taskId, priority)
    } catch {
      case e: Exception => 
        println(s"提取任务信息时出错: ${e.getMessage}, 尝试使用备选方案...")
        fallbackExtractTaskInfo(responseBody)
    }
  }
  
  // 备选方案提取任务信息
  private def fallbackExtractTaskInfo(responseBody: String): (String, String) = {
    try {
      val taskIdPattern = """"task_id":\s*"([^"]+)"""".r
      val priorityPattern = """"task_priority":\s*(\d+)""".r
      
      // 使用toSeq.headOption.getOrElse代替直接使用findFirstMatchIn，更安全
      val taskId = taskIdPattern.findAllMatchIn(responseBody).toSeq.headOption.map(_.group(1)).getOrElse("")
      val priority = priorityPattern.findAllMatchIn(responseBody).toSeq.headOption.map(_.group(1)).getOrElse("0")
      
      (taskId, priority)
    } catch {
      case e: Exception => 
        println(s"备选方案提取任务信息时出错: ${e.getMessage}, 响应体: ${responseBody.take(100)}...")
        ("", "0")
    }
  }
} 