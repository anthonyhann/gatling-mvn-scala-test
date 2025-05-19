name := "gatling-test"
version := "1.0"
scalaVersion := "2.13.8"

// Gatling依赖版本
val gatlingVersion = "3.8.4"

// 禁用打包文档和源码
Compile / packageDoc / publishArtifact := false
Compile / packageSrc / publishArtifact := false

// 添加Gatling依赖
libraryDependencies ++= Seq(
  "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion % "test,it",
  "io.gatling"            % "gatling-test-framework"    % gatlingVersion % "test,it",
  "com.typesafe.play"    %% "play-json"                % "2.9.4"         % "test,it"
)

// 启用Gatling插件
enablePlugins(GatlingPlugin)

// 设置JVM选项
scalacOptions := Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps"
)

// 设置测试目录
Test / resourceDirectory := baseDirectory.value / "src" / "test" / "resources" 