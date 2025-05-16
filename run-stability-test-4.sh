#!/bin/bash

# 设置JVM参数
export JAVA_OPTS="-Dgatling.ssl.useOpenSsl=false -Dio.netty.handler.ssl.openssl.useOpenSsl=false -Xmx1G -Duser.language=zh -Duser.country=CN -Dgatling.core.i18n.lang=zh-CN -Dfile.encoding=UTF-8"

# 运行压力测试,最高QPS达到200
echo "开始执行压力测试，峰值QPS: 250/s"
./mvnw clean gatling:test -Dgatling.simulationClass=example.PrintServiceStabilityTest \
   -Duser.language=zh \
   -Duser.country=CN \
   -Dgatling.core.i18n.lang=zh-CN \
   -Dfile.encoding=UTF-8 \
   -DVM_OPTS="-Dgatling.ssl.useOpenSsl=false -Dio.netty.handler.ssl.openssl.useOpenSsl=false"