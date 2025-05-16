#!/bin/bash

# 设置JVM参数
export JAVA_OPTS="-Dgatling.ssl.useOpenSsl=false -Dio.netty.handler.ssl.openssl.useOpenSsl=false -Xmx1G -Duser.language=zh -Duser.country=CN -Dgatling.core.i18n.lang=zh-CN -Dfile.encoding=UTF-8"

# 运行压力测试
./mvnw clean gatling:test -Dgatling.simulationClass=example.PrintServiceTest \
   -Duser.language=zh \
   -Duser.country=CN \
   -Dgatling.core.i18n.lang=zh-CN \
   -Dfile.encoding=UTF-8 \
   -DVM_OPTS="-Dgatling.ssl.useOpenSsl=false -Dio.netty.handler.ssl.openssl.useOpenSsl=false"