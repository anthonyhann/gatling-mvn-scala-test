package example

/**
 * 监控服务器启动类
 * 用于启动实时监控服务器
 */
object MonitorServer {
  /**
   * 程序入口点
   * 用于启动监控服务器
   */
  def main(args: Array[String]): Unit = {
    val port = if (args.length > 0) args(0).toInt else 8080
    
    println(s"正在启动实时监控服务器，端口: $port")
    
    // 启动监控服务器
    try {
      // 注册关闭钩子
      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run(): Unit = {
          println("正在关闭监控服务器...")
          RealTimeMonitorServer.stop()
        }
      })
      
      // 启动服务器
      RealTimeMonitorServer.start(port)
      println(s"监控服务器已启动: http://localhost:$port")
      
      // 等待服务器运行
      while (RealTimeMonitorServer.isRunning) {
        Thread.sleep(1000)
      }
    } catch {
      case e: Exception =>
        println(s"启动监控服务器时出错: ${e.getMessage}")
        e.printStackTrace()
    }
  }
} 