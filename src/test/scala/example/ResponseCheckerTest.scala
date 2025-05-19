package example

object ResponseCheckerTest {
  def main(args: Array[String]): Unit = {
    // 测试成功响应
    val successResponse = """{"code":"200","msg":"success","data":{"task_id":"12345","task_priority":1}}"""
    println(s"Success response check: ${ResponseChecker.isResponseSuccessful(successResponse)}")
    val (taskId, priority) = ResponseChecker.extractTaskInfo(successResponse)
    println(s"Extracted task info: taskId=$taskId, priority=$priority")
    
    // 测试错误码响应
    val errorResponse = """{"code":"5051","msg":"云盒USB未插入打印机","data":{}}"""
    println(s"Error response check: ${ResponseChecker.isResponseSuccessful(errorResponse)}")
    
    // 测试"设备未归属对应平台"响应
    val platformResponse = """{"code":"11013","msg":"设备未归属对应平台","data":{}}"""
    println(s"Platform response check: ${ResponseChecker.isResponseSuccessful(platformResponse)}")
    
    // 测试无效JSON响应
    val invalidResponse = "Connection reset"
    println(s"Invalid response check: ${ResponseChecker.isResponseSuccessful(invalidResponse)}")
    
    // 测试恶意JSON响应
    val malformedResponse = """{"code":"200","msg":"success","data":{"task_id":"""
    println(s"Malformed response check: ${ResponseChecker.isResponseSuccessful(malformedResponse)}")
    val (brokenTaskId, brokenPriority) = ResponseChecker.extractTaskInfo(malformedResponse)
    println(s"Extracted from broken: taskId=$brokenTaskId, priority=$brokenPriority")
  }
} 