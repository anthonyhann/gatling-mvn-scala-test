#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 检测操作系统
detect_os() {
  if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "macos"
  elif [[ -f /etc/debian_version ]]; then
    echo "debian"
  elif [[ -f /etc/redhat-release ]]; then
    echo "redhat"
  else
    echo "unknown"
  fi
}

# 安装SBT
install_sbt() {
  OS=$(detect_os)
  
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${BLUE}                          安装SBT - Scala构建工具                            ${NC}"
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  
  case $OS in
    macos)
      echo -e "${YELLOW}检测到macOS系统${NC}"
      if command -v brew &> /dev/null; then
        echo -e "${GREEN}使用Homebrew安装SBT...${NC}"
        brew install sbt
      else
        echo -e "${YELLOW}Homebrew未安装，正在安装Homebrew...${NC}"
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
        echo -e "${GREEN}使用Homebrew安装SBT...${NC}"
        brew install sbt
      fi
      ;;
      
    debian)
      echo -e "${YELLOW}检测到Debian/Ubuntu系统${NC}"
      echo -e "${GREEN}添加SBT软件源...${NC}"
      echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
      echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
      curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
      echo -e "${GREEN}更新软件包列表...${NC}"
      sudo apt-get update
      echo -e "${GREEN}安装SBT...${NC}"
      sudo apt-get install sbt
      ;;
      
    redhat)
      echo -e "${YELLOW}检测到CentOS/RHEL系统${NC}"
      echo -e "${GREEN}添加SBT软件源...${NC}"
      curl https://bintray.com/sbt/rpm/rpm | sudo tee /etc/yum.repos.d/bintray-sbt-rpm.repo
      echo -e "${GREEN}安装SBT...${NC}"
      sudo yum install sbt
      ;;
      
    *)
      echo -e "${RED}无法自动识别操作系统，请手动安装SBT${NC}"
      echo -e "${YELLOW}请访问 https://www.scala-sbt.org/download.html 获取安装指南${NC}"
      exit 1
      ;;
  esac
  
  # 验证安装
  if command -v sbt &> /dev/null; then
    echo -e "${GREEN}SBT安装成功!${NC}"
    sbt --version
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}现在可以运行 ./run-test.sh 启动性能测试了${NC}"
  else
    echo -e "${RED}SBT安装似乎失败，请手动安装${NC}"
    echo -e "${YELLOW}请访问 https://www.scala-sbt.org/download.html 获取安装指南${NC}"
    exit 1
  fi
}

# 检查是否已安装SBT
if command -v sbt &> /dev/null; then
  echo -e "${GREEN}SBT已经安装，版本信息:${NC}"
  sbt --version
  echo -e "${GREEN}您可以运行 ./run-test.sh 启动性能测试${NC}"
else
  echo -e "${YELLOW}未检测到SBT命令，需要安装SBT才能运行测试${NC}"
  read -p "是否立即安装SBT? (y/n): " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    install_sbt
  else
    echo -e "${YELLOW}您选择不安装SBT。如需手动安装，请访问:${NC}"
    echo -e "https://www.scala-sbt.org/download.html"
    exit 0
  fi
fi 