package example

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import scala.Console._

object DebugHelper {
  // 打印会话信息
  def printSession(prefix: String = ""): ChainBuilder = {
    exec(session => {
      println(s"""
        |${BLUE}========== $prefix 会话信息 ==========
        |设备ID: ${session("deviceId").as[String]}
        |设备密钥: ${session("deviceKey").as[String]}
        |HTTP状态: ${session.attributes.get("httpStatus").getOrElse("N/A")}
        |响应时间: ${session.attributes.get("responseTime").getOrElse("N/A")} ms
        |任务ID: ${session.attributes.get("taskId").getOrElse("N/A")}
        |任务优先级: ${session.attributes.get("taskPriority").getOrElse("N/A")}
        |是否成功: ${session.attributes.get("isSuccessful").getOrElse("N/A")}
        |完整会话数据: ${session}
        |${RESET}""".stripMargin)
      session
    })
  }

  // 断点暂停
  def breakpoint(message: String = "按回车继续..."): ChainBuilder = {
    exec(session => {
      println(s"${RED}断点: $message${RESET}")
      scala.io.StdIn.readLine()
      session
    })
  }

  // 条件断点
  def conditionalBreakpoint(condition: Session => Boolean, message: String = "条件断点触发，按回车继续..."): ChainBuilder = {
    exec(session => {
      if (condition(session)) {
        println(s"${RED}$message${RESET}")
        scala.io.StdIn.readLine()
      }
      session
    })
  }

  // 打印响应内容
  def printResponse: ChainBuilder = {
    exec(session => {
      val statusCode = session("httpStatus").as[Int]
      val responseBody = session("responseBody").as[String]
      println(s"""
        |${GREEN}========== 响应内容 ==========
        |状态码: $statusCode
        |响应体: $responseBody
        |${RESET}""".stripMargin)
      session
    })
  }

  // 打印请求内容
  def printRequest(requestName: String): ChainBuilder = {
    exec(session => {
      println(s"""
        |${YELLOW}========== 请求信息 [$requestName] ==========
        |设备ID: ${session("deviceId").as[String]}
        |设备密钥: ${session("deviceKey").as[String]}
        |${RESET}""".stripMargin)
      session
    })
  }
} 