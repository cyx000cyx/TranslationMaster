# 多语言音频翻译系统

基于Java微服务架构的企业级多语言音频翻译解决方案，支持语音识别、文本翻译、编码压缩等功能。

## 系统概述

本系统采用Kafka消息队列驱动的异步微服务架构，支持大规模音频文件的批量翻译处理。系统具备良好的扩展性和容错能力，支持多种语音识别模型和翻译服务的灵活切换。

### 核心特性

- **异步处理架构**: 基于Kafka消息队列的事件驱动架构
- **模型解耦设计**: 支持Whisper、其他ASR模型和多种LLM的切换
- **内存自适应**: 服务根据内存使用率自动调节消费速度
- **高可扩展性**: 微服务架构支持独立扩展和部署
- **多语言支持**: 支持中英日等数种语言的翻译
- **批量处理**: 支持音频文件夹的批量翻译处理

### 技术栈

- **后端框架**: Spring Boot 2.7.x, Spring Cloud 2021.x
- **消息队列**: Apache Kafka
- **数据库**: MySQL 8.0 + MyBatis-Plus
- **语音识别**: Whisper AI (可扩展)
- **文本翻译**: DeepSeek API (可扩展为GPT、Claude等)
- **文本压缩**: Snappy算法
- **构建工具**: Maven 3.6+
- **JDK版本**: Java 11+

## 项目结构

```
multi-language-translation-system/
├── doc/                        # 文档目录
├── common/                     # 公共模块
│   └── src/main/java/com/translation/common/
│       ├── kafka/             # Kafka消息定义
│       ├── response/          # 统一响应格式
│       └── utils/             # 工具类
├── task-service/              # 任务服务 (系统入口)
│   ├── src/main/java/com/translation/task/
│   │   ├── controller/        # REST API控制器
│   │   ├── service/           # 业务逻辑
│   │   ├── entity/            # 数据库实体
│   │   ├── mapper/            # MyBatis映射
│   │   └── config/            # 配置类
│   └── src/main/resources/
│       └── application.yml    # 配置文件
├── speech-service/            # 语音识别服务
│   ├── src/main/java/com/translation/speech/
│   │   ├── service/           # 语音识别接口
│   │   └── consumer/          # Kafka消息消费者
│   └── scripts/
│       └── whisper_processor.py  # Whisper处理脚本
├── translate-service/         # 翻译服务
│   ├── src/main/java/com/translation/translate/
│   │   ├── service/           # 翻译接口
│   │   └── consumer/          # Kafka消息消费者
│   └── src/main/resources/
├── encoding-service/          # 编码压缩服务
│   └── src/main/java/com/translation/encoding/
├── SQL/                       # 数据库脚本
│   └── translation_task.sql  # 数据库表结构
├── audio-source/              # 音频文件目录
├── web-frontend/              # Web前端界面
└── pom.xml                    # 主POM文件
```

## 快速开始

### 前置要求

1. Java 11+
2. Maven 3.6+
3. MySQL 8.0+
4. Python 3.8+ (用于Whisper)
5. Kafka 2.8+ (可选，系统支持内嵌模式)

### 环境准备

```bash
# 1. 克隆项目
git clone <repository-url>
cd multi-language-translation-system

# 2. 安装Python依赖
pip install openai-whisper torch torchaudio

# 3. 创建数据库
mysql -u root -p
CREATE DATABASE translation_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 4. 执行数据库脚本
mysql -u root -p translation_db < SQL/translation_task.sql
```

### 配置说明

1. **数据库配置**: 修改各服务的`application.yml`中的数据库连接信息
2. **DeepSeek API**: 在`translate-service`中配置API密钥
3. **音频路径**: 确保`audio-source`目录存在并包含MP3文件

### 启动服务

```bash
# 1. 编译整个项目
mvn clean install -DskipTests

# 2. 启动Task Service (系统入口)
cd task-service
mvn spring-boot:run -Dspring-boot.run.args="--server.port=8001"

# 3. 启动Speech Service (新终端)
cd speech-service
mvn spring-boot:run -Dspring-boot.run.args="--server.port=8002"

# 4. 启动Translation Service (新终端)
cd translate-service
mvn spring-boot:run -Dspring-boot.run.args="--server.port=8003"

# 5. 启动Encoding Service (新终端)
cd encoding-service
mvn spring-boot:run -Dspring-boot.run.args="--server.port=8004"
```

### 测试系统

1. **Web界面**: 打开`web-frontend/index.html`
2. **API测试**: 
```bash
curl -X POST http://localhost:8001/api/tasks/audio/create \
  -H "Content-Type: application/json" \
  -d '{
    "audioDirectory": "Tilly'\''s Lost Balloon",
    "sourceLanguage": "en",
    "targetLanguages": "zh-CN,ja,ko",
    "priority": 5
  }'
```