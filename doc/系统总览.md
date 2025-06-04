# 系统总览

## 架构重构总结

系统已从网关架构重构为基于Kafka消息队列的异步微服务架构，实现了以下核心目标：

### 重构完成的功能

1. **删除网关服务** - 移除Spring Cloud Gateway，简化架构
2. **Task Service作为系统入口** - 运行在8001端口，提供REST API
3. **Speech Service解耦Whisper** - 支持多种ASR模型切换
4. **Translation Service解耦DeepSeek** - 支持多种LLM模型切换
5. **内存自适应消费** - 服务达到50%堆内存时停止消费
6. **完整Kafka消息流** - Task → Speech → Translation → Encoding

### 核心特性

- **异步处理**: 基于Kafka的事件驱动架构
- **模型抽象**: 支持ASR和LLM模型的灵活切换  
- **内存管理**: 智能内存监控和消费控制
- **错误处理**: 完善的错误传播和恢复机制
- **可扩展性**: 支持水平扩展和负载均衡

## 服务架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Task Service  │    │  Speech Service │    │Translation Svc  │    │ Encoding Service│
│   (入口:8001)    │    │     (8002)      │    │     (8003)      │    │     (8004)      │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │                      │
          ▼                      ▼                      ▼                      ▼
    ┌─────────────────────────────────────────────────────────────────────────────────┐
    │                           Kafka Message Queue                                    │
    │  task.created → speech.completed → translation.completed → encoding.completed   │
    └─────────────────────────────────────────────────────────────────────────────────┘
```

## 技术栈

| 组件 | 技术选型 | 版本 |
|------|----------|------|
| 应用框架 | Spring Boot | 2.7.18 |
| 消息队列 | Apache Kafka | 3.6.1 |
| 数据库 | MySQL | 8.0+ |
| ORM框架 | MyBatis-Plus | 3.5.5 |
| 语音识别 | Whisper AI | Latest |
| 文本翻译 | DeepSeek API | Latest |
| 构建工具 | Maven | 3.6+ |
| 运行环境 | Java | 11+ |

## 部署架构

### 开发环境
```
localhost:8001 - Task Service (系统入口)
localhost:8002 - Speech Service  
localhost:8003 - Translation Service
localhost:8004 - Encoding Service
localhost:3306 - MySQL Database
localhost:9092 - Kafka Broker
```

### 生产环境建议
```
Load Balancer → Multiple Task Service Instances
                     ↓
            Kafka Cluster (3+ brokers)
                     ↓
   Speech Service    Translation Service    Encoding Service
   (3+ instances)    (3+ instances)        (2+ instances)
                     ↓
            MySQL Master-Slave Cluster
```

## 关键文件结构

```
project/
├── doc/                          # 完整文档
│   ├── README.md                 # 项目总览
│   ├── architecture.md           # 架构设计  
│   ├── deployment.md             # 部署指南
│   ├── api.md                    # API文档
│   ├── call-flow.md              # 调用链路
│   ├── troubleshooting.md        # 故障排除
│   └── system-overview.md        # 本文档
├── common/                       # 公共组件
│   └── src/main/java/com/translation/common/
│       └── kafka/                # Kafka消息定义
├── task-service/                 # 任务服务 (系统入口)
├── speech-service/               # 语音识别服务
├── translate-service/            # 翻译服务  
├── encoding-service/             # 编码服务
├── SQL/                          # 数据库脚本
├── audio-source/                 # 音频文件目录
└── web-frontend/                 # Web测试界面
```

## 数据流转

### 完整处理链路
```
1. 用户创建任务 → Task Service API
2. 保存任务到数据库 → MySQL
3. 发送消息 → Kafka: task.created
4. Speech Service消费 → 执行Whisper识别
5. 保存识别结果 → 文件系统
6. 发送消息 → Kafka: speech.recognition.completed  
7. Translation Service消费 → 调用DeepSeek API
8. 保存翻译结果 → 文件系统
9. 发送消息 → Kafka: translation.completed
10. Encoding Service消费 → Snappy压缩
11. 保存编码结果 → 文件系统
12. 发送消息 → Kafka: encoding.completed
13. Task Service更新状态 → 任务完成
```

### 错误处理链路
```
任何阶段失败 → 发送task.failed消息 → Task Service更新为FAILED状态
```

## API接口总览

### Task Service API (端口8001)

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/tasks/audio/create | 创建音频翻译任务 |
| GET  | /api/tasks/{taskId} | 查询任务状态 |
| POST | /api/tasks/{taskId}/cancel | 取消任务 |
| GET  | /api/tasks/list | 获取任务列表 |
| GET  | /api/tasks/audio/folders | 获取音频文件夹 |

### 请求示例
POST /api/tasks/audio/create
```json
{
  "audioDirectory": "Tilly's Lost Balloon",
  "sourceLanguage": "en",
  "targetLanguages": "zh-CN,ja,ko",
  "priority": 5
}
```

## 配置要点

### 必需配置

1. **数据库连接** (各服务application.yml)
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/translation_db
    username: translation_user
    password: your_password
```

2. **Kafka连接** (各服务application.yml)  
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

3. **DeepSeek API密钥** (translate-service)
```yaml
translation:
  deepseek:
    api:
      key: ${DEEPSEEK_API_KEY}
```

4. **音频文件路径** (task-service)
```yaml
audio:
  source:
    path: ./audio-source
```

### 环境变量
```bash
export DEEPSEEK_API_KEY="your_deepseek_api_key"
export DATABASE_URL="jdbc:mysql://localhost:3306/translation_db"
export KAFKA_SERVERS="localhost:9092"
```

## 启动顺序

### 基础服务
1. MySQL数据库
2. Kafka服务 (包含Zookeeper)

### 应用服务
1. Task Service (8001) - 系统入口
2. Speech Service (8002)  
3. Translation Service (8003)
4. Encoding Service (8004)

### 启动命令
```bash
# 编译项目
mvn clean install -DskipTests

# 启动各服务 (分别在不同终端)
cd task-service && mvn spring-boot:run -Dspring-boot.run.args="--server.port=8001"
cd speech-service && mvn spring-boot:run -Dspring-boot.run.args="--server.port=8002"  
cd translate-service && mvn spring-boot:run -Dspring-boot.run.args="--server.port=8003"
cd encoding-service && mvn spring-boot:run -Dspring-boot.run.args="--server.port=8004"
```

## 测试验证

### 1. 健康检查
```bash
curl http://localhost:8001/actuator/health
curl http://localhost:8002/actuator/health
curl http://localhost:8003/actuator/health
curl http://localhost:8004/actuator/health
```

### 2. 功能测试
```bash
# 获取音频文件夹
curl http://localhost:8001/api/tasks/audio/folders

# 创建测试任务
curl -X POST http://localhost:8001/api/tasks/audio/create \
  -H "Content-Type: application/json" \
  -d '{
    "audioDirectory": "test_folder",
    "sourceLanguage": "en", 
    "targetLanguages": "zh-CN,ja",
    "priority": 5
  }'
```

### 3. Web界面测试
打开 `web-frontend/index.html` 进行可视化测试

## 性能特征

### 处理能力
- **并发任务**: 10-50个 (取决于硬件)
- **音频识别**: 约为音频时长的0.1-0.3倍
- **翻译速度**: 1000字符/秒
- **API响应**: 平均200ms以内

### 资源需求
- **内存**: 最低4GB，推荐8GB+
- **CPU**: 最低4核，推荐8核+  
- **磁盘**: 最低50GB，推荐100GB+ SSD
- **网络**: 稳定互联网连接 (DeepSeek API)

## 扩展计划

### 模型扩展
```
// ASR模型扩展
SpeechRecognitionService
├── WhisperSpeechRecognitionServiceImpl (当前)
├── GoogleCloudSpeechServiceImpl (计划)
└── AzureSpeechServiceImpl (计划)

// 翻译模型扩展  
TranslationService
├── DeepSeekTranslationServiceImpl (当前)
├── OpenAITranslationServiceImpl (计划)
└── ClaudeTranslationServiceImpl (计划)
```

### 架构扩展
- 支持实时流式处理
- 添加缓存层 (Redis)
- 支持多租户
- 添加监控面板 (Grafana)
- 支持容器化部署 (Docker/K8s)

## 注意事项

### 外部依赖
1. **DeepSeek API** - 需要有效的API密钥和账户余额
2. **Python环境** - Whisper需要Python 3.8+和相关依赖
3. **网络连接** - 翻译功能需要稳定的互联网连接

### 数据完整性
- 系统仅处理真实音频文件，不使用模拟数据
- 翻译结果来自真实的DeepSeek API响应
- 所有处理结果保存为实际文件

### 安全考虑
- API密钥通过环境变量配置
- 数据库连接使用专用用户账户
- 文件访问权限适当限制