package example

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

/**
 * Gatling测试启动器
 * 用于启动指定的测试类
 */
object TestLauncher {
  
  def main(args: Array[String]): Unit = {
    println("启动Gatling测试...")
    
    // 获取要运行的测试类
    val simulationClass = args.headOption.getOrElse("example.OptimizedPrintServiceTest")
    println(s"运行测试类: $simulationClass")
    
    // 构建Gatling属性
    val props = new GatlingPropertiesBuilder()
      .resourcesDirectory("src/test/resources")
      .resultsDirectory("target/gatling/results")
      .simulationClass(simulationClass)
      .build
    
    // 启动Gatling
    Gatling.fromMap(props)
    
    println("测试完成")
  }
} 