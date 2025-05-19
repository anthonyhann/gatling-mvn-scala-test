package example

// 不导入Gatling相关的类
import play.api.libs.json._

/**
 * 简化版响应检查器
 * 仅用于基础测试
 */
object ResponseCheckerSimpleTest {
  def isResponseSuccessful(responseBody: String): Boolean = {
    try {
      val json = Json.parse(responseBody)
      val code = (json \ "code").asOpt[String]
      val msg = (json \ "msg").asOpt[String]
      
      // 简化的成功判断
      code.contains("200") || msg.contains("success")
    } catch {
      case e: Exception => 
        println(s"解析响应时出错: ${e.getMessage}")
        false
    }
  }
  
  def extractTaskInfo(responseBody: String): (String, String) = {
    try {
      val json = Json.parse(responseBody)
      val data = (json \ "data").asOpt[JsObject]
      
      val taskId = data.flatMap(d => (d \ "taskId").asOpt[String]).getOrElse("N/A")
      val priority = data.flatMap(d => (d \ "priority").asOpt[String]).getOrElse("0")
      
      (taskId, priority)
    } catch {
      case e: Exception => 
        println(s"提取任务信息时出错: ${e.getMessage}")
        ("N/A", "N/A")
    }
  }
} 