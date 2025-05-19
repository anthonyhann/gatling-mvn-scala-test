#!/bin/bash

# 打印服务性能测试运行脚本
# 作者：2024
# 版本：1.0

# 默认值
TEST_CLASS="OptimizedPrintServiceTest"
ENV="dev"
MONITOR="true"
DEBUG="false"
OPEN_BROWSER="true"

# 显示帮助信息
show_help() {
  echo "打印服务性能测试运行脚本"
  echo ""
  echo "用法: $0 [options] 或 $0 <test-type>"
  echo ""
  echo "test-type 支持以下快捷方式:"
  echo "  - benchmark: 基准测试 (OptimizedPrintServiceTest)"
  echo "  - load: 负载测试 (PrintServiceLoadTest)"
  echo "  - stress: 压力测试 (PrintServiceStressTest)"
  echo "  - stability: 稳定性测试 (PrintServiceStabilityTest)" 
  echo ""
  echo "选项:"
  echo "  -t, --test CLASS        指定测试类或使用快捷方式 (默认: $TEST_CLASS)"
  echo "  -e, --env ENV           指定环境 [dev|test|prod] (默认: $ENV)"
  echo "  -m, --monitor           启用监控 (默认: $MONITOR)"
  echo "  -n, --no-monitor        禁用监控"
  echo "  -d, --debug             启用调试模式"
  echo "  -b, --no-browser        不自动打开浏览器"
  echo "  -h, --help              显示此帮助信息"
  echo ""
  echo "例子:"
  echo "  $0 benchmark            运行基准测试"
  echo "  $0 -t benchmark         运行基准测试(同上)"
  echo "  $0 --test PrintServiceLoadTest --env test    在测试环境运行负载测试"
  echo "  $0 stability -n         运行稳定性测试，禁用监控"
}

# 处理快捷方式
if [ $# -eq 1 ] && [[ ! "$1" =~ ^- ]]; then
  case "$1" in
    benchmark)
      TEST_CLASS="OptimizedPrintServiceTest"
      ;;
    load)
      TEST_CLASS="PrintServiceLoadTest"
      ;;
    stress)
      TEST_CLASS="PrintServiceStressTest"
      ;;
    stability)
      TEST_CLASS="PrintServiceStabilityTest"
      ;;
    help)
      show_help
      exit 0
      ;;
    *)
      echo "未知的测试类型: $1"
      echo "可用类型: benchmark, load, stress, stability"
      exit 1
      ;;
  esac
  shift
else
  # 处理命令行参数
  while [ "$#" -gt 0 ]; do
    case "$1" in
      -t|--test)
        # 处理测试类型快捷方式
        case "$2" in
          benchmark)
            TEST_CLASS="OptimizedPrintServiceTest"
            ;;
          load)
            TEST_CLASS="PrintServiceLoadTest"
            ;;
          stress)
            TEST_CLASS="PrintServiceStressTest"
            ;;
          stability)
            TEST_CLASS="PrintServiceStabilityTest"
            ;;
          *)
            TEST_CLASS="$2"
            ;;
        esac
        shift 2
        ;;
      -e|--env)
        ENV="$2"
        shift 2
        ;;
      -m|--monitor)
        MONITOR="true"
        shift
        ;;
      -n|--no-monitor)
        MONITOR="false"
        shift
        ;;
      -d|--debug)
        DEBUG="true"
        shift
        ;;
      -b|--no-browser)
        OPEN_BROWSER="false"
        shift
        ;;
      -h|--help)
        show_help
        exit 0
        ;;
      *)
        echo "错误: 未知选项 $1"
        show_help
        exit 1
        ;;
    esac
  done
fi

# 验证测试类
if [[ ! "$TEST_CLASS" =~ ^(OptimizedPrintServiceTest|PrintServiceTest|PrintServiceLoadTest|PrintServiceStressTest|PrintServiceStabilityTest)$ ]]; then
  echo "警告: '$TEST_CLASS' 不是标准测试类，确保此类存在"
fi

# 验证环境
if [[ ! "$ENV" =~ ^(dev|test|prod)$ ]]; then
  echo "警告: '$ENV' 不是标准环境，确保对应配置文件存在"
fi

# 打印测试信息
echo "=========================================="
echo "  打印服务性能测试"
echo "=========================================="
echo "测试类:     $TEST_CLASS"
echo "环境:       $ENV"
echo "启用监控:   $MONITOR"
echo "调试模式:   $DEBUG"
echo "自动打开浏览器: $OPEN_BROWSER"
echo "=========================================="
echo "开始测试..."

# 检查SBT是否存在
if ! command -v sbt &> /dev/null; then
  echo "错误: 未找到 sbt 命令。请先安装 sbt。"
  echo "可以使用 './install-sbt.sh' 安装。"
  exit 1
fi

# 保存开始时间
START_TIME=$(date +%s)

# 设置JAVA选项
export SBT_OPTS="-Xmx2G -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 运行测试
sbt "gatling:testOnly example.$TEST_CLASS" \
    -Denv=$ENV \
    -Dmonitor=$MONITOR \
    -Ddebug=$DEBUG

# 检查测试结果
if [ $? -eq 0 ]; then
  TEST_RESULT="成功"
else
  TEST_RESULT="失败"
fi

# 计算运行时间
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
MINUTES=$((DURATION / 60))
SECONDS=$((DURATION % 60))

echo "=========================================="
echo "测试完成: $TEST_RESULT"
echo "运行时间: ${MINUTES}分${SECONDS}秒"
echo "=========================================="

# 查找报告目录
GATLING_REPORT=$(ls -td target/gatling/results/*/index.html 2>/dev/null | head -1)
ENHANCED_REPORT=$(ls -td target/gatling/enhanced-reports/$TEST_CLASS-*/report.html 2>/dev/null | head -1)

if [ -n "$ENHANCED_REPORT" ]; then
  echo "增强报告生成于: $ENHANCED_REPORT"
  
  # 自动打开报告
  if [ "$OPEN_BROWSER" = "true" ]; then
    if [ "$(uname)" == "Darwin" ]; then
      # 打开HTML报告
      open "$ENHANCED_REPORT"
      # 打开实时监控页面
      open "http://localhost:8080"
    elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
      if command -v xdg-open > /dev/null; then
        # 打开HTML报告
        xdg-open "$ENHANCED_REPORT"
        # 打开实时监控页面
        xdg-open "http://localhost:8080"
      fi
    fi
  fi
elif [ -n "$GATLING_REPORT" ]; then
  echo "Gatling报告生成于: $GATLING_REPORT"
  
  # 自动打开报告
  if [ "$OPEN_BROWSER" = "true" ]; then
    if [ "$(uname)" == "Darwin" ]; then
      # 打开HTML报告
      open "$GATLING_REPORT"
      # 打开实时监控页面
      open "http://localhost:8080"
    elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
      if command -v xdg-open > /dev/null; then
        # 打开HTML报告
        xdg-open "$GATLING_REPORT"
        # 打开实时监控页面
        xdg-open "http://localhost:8080"
      fi
    fi
  fi
else
  echo "报告未找到，请检查测试输出"
  
  # 尝试打开实时监控页面
  if [ "$OPEN_BROWSER" = "true" ] && [ "$MONITOR" = "true" ]; then
    if [ "$(uname)" == "Darwin" ]; then
      open "http://localhost:8080"
    elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
      if command -v xdg-open > /dev/null; then
        xdg-open "http://localhost:8080"
      fi
    fi
  fi
fi

exit 0 