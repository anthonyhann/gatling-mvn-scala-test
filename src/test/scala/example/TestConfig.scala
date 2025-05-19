package example

import java.io.{File, FileInputStream}
import java.util.Properties
import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * 测试配置管理类
 * 负责加载和管理测试配置，支持多环境配置
 */
object TestConfig {
  
  // 配置缓存
  private val configCache = mutable.Map[String, TestConfig]()
  
  /**
   * 加载指定环境的配置
   * @param environment 环境名称 (dev, test, prod)
   * @return 测试配置对象
   */
  def load(environment: String = "dev"): TestConfig = {
    // 如果缓存中已有配置，直接返回
    if (configCache.contains(environment)) {
      return configCache(environment)
    }
    
    // 加载默认配置
    val properties = new Properties()
    loadProperties(properties, "src/test/resources/test-config.properties")
    
    // 加载环境特定配置（如果存在）
    val envConfigFile = s"src/test/resources/environments/$environment.properties"
    if (new File(envConfigFile).exists()) {
      loadProperties(properties, envConfigFile)
    } else {
      println(s"警告: 环境配置文件 $envConfigFile 不存在, 使用默认配置")
    }
    
    // 创建配置对象
    val config = new TestConfig(properties, environment)
    
    // 缓存配置
    configCache.put(environment, config)
    
    config
  }
  
  /**
   * 加载属性文件
   */
  private def loadProperties(properties: Properties, filePath: String): Unit = {
    try {
      val file = new File(filePath)
      if (file.exists()) {
        val inputStream = new FileInputStream(file)
        try {
          properties.load(inputStream)
        } finally {
          inputStream.close()
        }
      }
    } catch {
      case e: Exception => 
        println(s"警告: 加载配置文件 $filePath 失败: ${e.getMessage}")
    }
  }
  
  /**
   * 清除配置缓存
   */
  def clearCache(): Unit = {
    configCache.clear()
  }
}

/**
 * 测试配置类
 * 提供类型安全的配置访问方法
 */
class TestConfig(properties: Properties, val environment: String) {
  
  /**
   * 获取字符串配置值
   * @param key 配置键
   * @param defaultValue 默认值
   * @return 配置值或默认值
   */
  def getOrDefault(key: String, defaultValue: String): String = {
    Option(properties.getProperty(key)).getOrElse(defaultValue)
  }
  
  /**
   * 获取整数配置值
   * @param key 配置键
   * @param defaultValue 默认值
   * @return 配置值或默认值
   */
  def getIntOrDefault(key: String, defaultValue: Int): Int = {
    try {
      getOrDefault(key, defaultValue.toString).toInt
    } catch {
      case _: NumberFormatException => defaultValue
    }
  }
  
  /**
   * 获取双精度浮点数配置值
   * @param key 配置键
   * @param defaultValue 默认值
   * @return 配置值或默认值
   */
  def getDoubleOrDefault(key: String, defaultValue: Double): Double = {
    try {
      getOrDefault(key, defaultValue.toString).toDouble
    } catch {
      case _: NumberFormatException => defaultValue
    }
  }
  
  /**
   * 获取布尔配置值
   * @param key 配置键
   * @param defaultValue 默认值
   * @return 配置值或默认值
   */
  def getBooleanOrDefault(key: String, defaultValue: Boolean): Boolean = {
    val valueStr = getOrDefault(key, defaultValue.toString).toLowerCase
    valueStr match {
      case "true" | "yes" | "1" | "on" => true
      case "false" | "no" | "0" | "off" => false
      case _ => defaultValue
    }
  }
  
  /**
   * 获取特定前缀的配置集合
   * @param prefix 前缀
   * @return 配置键值对
   */
  def getConfigsWithPrefix(prefix: String): Map[String, String] = {
    properties.asScala
      .filter { case (key, _) => key.toString.startsWith(prefix) }
      .map { case (key, value) => (key.toString, value.toString) }
      .toMap
  }
  
  /**
   * 获取所有配置
   * @return 配置键值对
   */
  def getAllConfigs(): Map[String, String] = {
    properties.asScala.map { case (key, value) => (key.toString, value.toString) }.toMap
  }
} 