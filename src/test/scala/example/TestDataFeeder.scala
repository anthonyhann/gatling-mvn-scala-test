package example

import io.gatling.core.Predef._
import scala.io.Source
import java.io.File
import scala.util.{Try, Success, Failure}
import scala.util.Random

object TestDataFeeder {
  // 测试数据文件路径
  private val dataFilePath = "src/test/resources/test_samples.csv"
  private val devicesFilePath = "src/test/resources/data/devices.csv"
  
  // 确保测试数据目录存在
  private def ensureDirectoryExists(): Unit = {
    val dir = new File("src/test/resources")
    if (!dir.exists()) {
      dir.mkdirs()
    }
  }

  // 创建默认测试数据文件（如果不存在）
  private def createDefaultDataFile(): Unit = {
    ensureDirectoryExists()
    val file = new File(dataFilePath)
    if (!file.exists()) {
      val defaultData = 
        """deviceId,deviceKey
          |demo-device-9887,zmlpVpxQH6EAMn3x
          |demo-device-2639,yQjO0v6GWDyqam9H
          |demo-device-4073,C3L8wn5jXK86OcbD
          |demo-device-5792,YfAftauGmWdnpzVv
          |demo-device-8669,R5i1LULzfikljmIV
          |demo-device-1985,X0hYlerKcnyYyzxf
          |demo-device-3187,25SyBtOOt2SnYPps
          |demo-device-6739,UeB7pF42WH5p2zOL
          |demo-device-9782,rQL9PBx3vPR7lPkN
          |demo-device-5066,OU1o1vygGatPUkWB""".stripMargin
      
      Try {
        val pw = new java.io.PrintWriter(file)
        try pw.write(defaultData) finally pw.close()
      } match {
        case Success(_) => println("已创建默认测试数据文件")
        case Failure(e) => println(s"创建默认测试数据文件失败: ${e.getMessage}")
      }
    }
  }

  // 加载测试数据
  def loadTestData = {
    createDefaultDataFile() // 确保测试数据文件存在
    
    Try {
      val source = Source.fromFile(dataFilePath)
      try {
        // 跳过标题行，读取数据行
        val records = source.getLines().drop(1).map { line =>
          val fields = line.split(",").map(_.trim)
          Map(
            "deviceId" -> fields(0),
            "deviceKey" -> fields(1)
          )
        }.toArray
        
        // 转换为Gatling的feed格式
        records.circular
      } finally {
        source.close()
      }
    } match {
      case Success(feeder) => feeder
      case Failure(e) => 
        println(s"加载测试数据失败: ${e.getMessage}")
        // 返回默认数据作为备份
        Array(
          Map("deviceId" -> "demo-device-5555", "deviceKey" -> "demoKey0000000000")
        ).circular
    }
  }
  
  // 从devices.csv中随机抽样
  def loadRandomDevices(sampleSize: Int = 100) = {
    val devicesFile = new File(devicesFilePath)
    
    // 如果文件不存在，使用默认数据
    if (!devicesFile.exists()) {
      println(s"警告: 设备数据文件 $devicesFilePath 不存在，使用默认测试数据")
      loadTestData
    } else {
      // 尝试加载和抽样devices.csv
      val result = Try {
        val source = Source.fromFile(devicesFilePath)
        try {
          // 读取所有行（devices.csv没有标题行，每行格式为 deviceId,deviceKey）
          val allLines = source.getLines().toArray
          println(s"共从 $devicesFilePath 读取到 ${allLines.length} 个设备")
          
          // 过滤排除具有特定前缀的设备ID（可能是过期或有问题的设备）
          // 这里假设以"demo-device-5537"开头的设备是有效的，您可能需要调整此条件
          val filteredLines = allLines.filter(line => {
            val fields = line.split(",").map(_.trim)
            if (fields.length < 1) false
            else {
              val deviceId = fields(0)
              // 避免使用已知有问题的设备ID
              !deviceId.contains("demo-device-5537") 
            }
          })
          
          println(s"过滤后剩余 ${filteredLines.length} 个设备")
          
          // 如果过滤后设备太少，仍使用原始数据
          val linesToUse = if (filteredLines.length < sampleSize * 2) {
            println(s"过滤后设备数量不足(${filteredLines.length} < ${sampleSize * 2})，使用原始设备数据")
            allLines
          } else {
            filteredLines
          }
          
          // 随机选择指定数量的行
          val random = new Random()
          val sampledLines = if (linesToUse.length <= sampleSize) {
            println(s"设备总数 ${linesToUse.length} 小于等于要求的样本数 $sampleSize，使用全部设备")
            linesToUse
          } else {
            println(s"从 ${linesToUse.length} 个设备中随机抽样 $sampleSize 个设备")
            random.shuffle(linesToUse.toList).take(sampleSize).toArray
          }
          
          // 转换为Feeder格式
          val records = sampledLines.map { line =>
            val fields = line.split(",").map(_.trim)
            if (fields.length >= 2) {
              Map(
                "deviceId" -> fields(0),
                "deviceKey" -> fields(1)
              )
            } else {
              println(s"警告: 设备数据格式错误 '$line'，使用默认值")
              Map(
                "deviceId" -> "demo-device-5555",
                "deviceKey" -> "demoKey0000000000"
              )
            }
          }
          
          println(s"成功随机抽样 ${records.length} 个设备")
          // 转换为Gatling的feed格式，使用随机策略
          records.random
        } finally {
          source.close()
        }
      }
      
      // 处理结果
      result match {
        case Success(feeder) => feeder
        case Failure(e) => 
          println(s"从devices.csv随机抽样失败: ${e.getMessage}")
          println("使用默认测试数据作为备份")
          loadTestData
      }
    }
  }
} 