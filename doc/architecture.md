# 系统架构设计

## 架构概述

多语言音频翻译系统采用事件驱动的微服务架构，通过Kafka消息队列实现服务间的异步通信。系统具备高可扩展性、高可用性和良好的容错能力。

## 整体架构图

```
                    ┌─────────────────┐
                    │   Web Frontend  │
                    │   (静态页面)     │
                    └─────────┬───────┘
                              │ HTTP REST
                              ▼
┌──────────────────────────────────────────────────────────────┐
│                     Task Service                              │
│                    (系统入口 - 8001)                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌───────────────┐ │
│  │  Task Controller│  │   Task Service  │  │  Task Mapper  │ │
│  │   (REST API)    │  │  (业务逻辑)     │  │  (数据访问)   │ │
│  └─────────────────┘  └─────────────────┘  └───────────────┘ │
│              │                 │                            │
│              │                 ▼                            │
│              │         ┌─────────────────┐                  │
│              │         │    MySQL DB     │                  │
│              │         │  (任务状态管理)  │                  │
│              │         └─────────────────┘                  │
│              │                                              │
│              ▼                                              │
│      ┌─────────────────┐                                    │
│      │ Kafka Producer  │                                    │
│      │  (发送任务消息)   │                                    │
│      └─────────────────┘                                    │
└──────────────┬───────────────────────────────────────────────┘
               │
               ▼
    ┌─────────────────────┐
    │     Kafka Cluster   │
    │   (消息队列中心)     │
    │                     │
    │ Topics:             │
    │ - task.created      │
    │ - speech.completed  │
    │ - translation.done  │
    │ - encoding.done     │
    │ - task.failed       │
    └─────────┬───────────┘
              │
    ┌─────────┼─────────┬─────────┐
    │         │         │         │
    ▼         ▼         ▼         ▼
┌─────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ Speech  │ │Translation│ │ Encoding │ │   Task   │
│ Service │ │ Service   │ │ Service  │ │ Service  │
│ (8002)  │ │ (8003)    │ │ (8004)   │ │ (8001)   │
└─────────┘ └──────────┘ └──────────┘ └──────────┘
```

## 服务架构详解

### 1. Task Service (任务服务 - 系统入口)

**职责**: 
- 接收外部API请求
- 管理任务生命周期
- 发送Kafka消息启动处理流程
- 更新任务状态和进度

**核心组件**:
```
TaskController (REST API层)
├── createAudioTranslationTask()  // 创建音频翻译任务
├── getTaskStatus()               // 查询任务状态  
├── cancelTask()                  // 取消任务
└── getAudioFolders()            // 获取可用音频文件夹

TaskService (业务逻辑层)
├── 验证音频目录和文件
├── 创建数据库记录
├── 发送Kafka消息
└── 状态管理

TranslationTaskMapper (数据访问层)
└── MyBatis-Plus数据库操作
```

### 2. Speech Service (语音识别服务)

**职责**:
- 接收任务创建消息
- 执行音频文件的语音识别
- 支持多种ASR模型切换
- 内存自适应消费控制

**核心架构**:
```
TaskCreatedConsumer (Kafka消费者)
├── 内存使用率检查 (>50%停止消费)
├── 调用SpeechRecognitionService
└── 发送识别完成消息

SpeechRecognitionService (抽象接口)
└── WhisperSpeechRecognitionServiceImpl
    ├── 调用Python Whisper脚本
    ├── 批量处理音频文件
    ├── 保存识别结果到文件
    └── 返回结构化结果

MemoryAwareConsumer (内存监控基类)
├── shouldStopConsuming() // 检查内存阈值
└── forceGarbageCollection() // 强制GC
```

**模型切换支持**:
```java
// 可以轻松切换为其他ASR模型
public interface SpeechRecognitionService {
    SpeechRecognitionResult recognizeAudio(String audioFilePath, String language, Map<String, Object> options);
    BatchRecognitionResult batchRecognizeAudio(String audioDirectoryPath, String language, Map<String, Object> options);
    boolean isModelAvailable();
    String[] getSupportedLanguages();
}

// 实现类示例:
// - WhisperSpeechRecognitionServiceImpl (当前)
// - GoogleCloudSpeechServiceImpl (可扩展)
// - AzureSpeechServiceImpl (可扩展)
```

### 3. Translation Service (翻译服务)

**职责**:
- 接收语音识别完成消息
- 执行多语言文本翻译
- 支持多种LLM模型切换
- 保存翻译结果到文件

**核心架构**:
```
SpeechRecognitionCompletedConsumer (Kafka消费者)
├── 内存使用率检查
├── 调用TranslationService
├── 保存翻译结果
└── 发送翻译完成消息

TranslationService (抽象接口)
└── DeepSeekTranslationServiceImpl
    ├── HTTP调用DeepSeek API
    ├── 批量翻译到多种语言
    ├── 结果清理和验证
    └── 错误处理和重试

支持的翻译模型:
├── DeepSeek (当前实现)
├── OpenAI GPT (可扩展)
├── Claude (可扩展)
└── 本地模型 (可扩展)
```

### 4. Encoding Service (编码压缩服务)

**职责**:
- 接收翻译完成消息
- 执行Snappy压缩算法
- 文本编码和打包
- 完成任务流程

**核心功能**:
```
TextEncodingService
├── Snappy压缩算法
├── 多语言文本编码
├── 压缩率统计
└── 查询接口
```

## 消息流转设计

### 完整处理流程

```
1. [用户请求] → Task Service
   POST /api/tasks/audio/create
   {
     "audioDirectory": "folder_name",
     "sourceLanguage": "en", 
     "targetLanguages": "zh-CN,ja,ko"
   }

2. [Task Service] → Kafka Topic: task.created
   {
     "taskId": "uuid",
     "audioDirectoryPath": "/path/to/audio",
     "sourceLanguage": "en",
     "targetLanguages": "zh-CN,ja,ko",
     "taskType": "AUDIO_TRANSLATION",
     "priority": 5
   }

3. [Speech Service] ← Kafka Topic: task.created
   → 处理音频识别
   → 保存识别结果到文件
   → Kafka Topic: speech.recognition.completed

4. [Translation Service] ← Kafka Topic: speech.recognition.completed  
   → 执行多语言翻译
   → 保存翻译结果到文件
   → Kafka Topic: translation.completed

5. [Encoding Service] ← Kafka Topic: translation.completed
   → 执行文本编码压缩
   → 保存最终结果
   → Kafka Topic: encoding.completed

6. [Task Service] ← Kafka Topic: encoding.completed
   → 更新任务状态为完成
   → 记录处理统计信息
```

### 错误处理流程

```
任何服务处理失败 → Kafka Topic: task.failed
                  ↓
Task Service 接收失败消息 → 更新任务状态为FAILED
                        → 记录错误信息
                        → 停止后续处理
```

## 内存管理策略

### 自适应消费控制

理论来说熔断和降频应该是在80%左右内存占用时才触发, 这里的50%是为了测试效果.

```java
// 每个消费者都继承MemoryAwareConsumer
public abstract class MemoryAwareConsumer {
    private static final double MEMORY_THRESHOLD = 0.5; // 50%阈值
    
    protected boolean shouldStopConsuming() {
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsageRatio = (double) usedMemory / maxMemory;
        
        if (memoryUsageRatio > MEMORY_THRESHOLD) {
            log.warn("内存使用率超过50%，暂停消息消费");
            return true;
        }
        return false;
    }
}
```

### 内存监控策略

1. **实时监控**: 每次消费消息前检查内存使用率
2. **动态调节**: 超过阈值时暂停消费，触发GC
3. **优雅恢复**: 内存释放后自动恢复消费
4. **日志记录**: 详细记录内存使用情况

## 扩展性设计

### 水平扩展

```
1. 服务扩展:
   - 每个微服务支持多实例部署
   - Kafka消费者组自动负载均衡
   - 数据库连接池动态调整

2. 消息分区:
   - 按任务ID进行分区
   - 保证同一任务消息有序处理
   - 支持增加分区提升并发

3. 存储扩展:
   - 音频文件分布式存储
   - 数据库读写分离
   - 结果文件云存储
```

### 模型扩展

```
1. ASR模型扩展:
   interface SpeechRecognitionService
   ├── WhisperSpeechRecognitionServiceImpl
   ├── GoogleCloudSpeechServiceImpl  (新增)
   └── AzureSpeechServiceImpl        (新增)

2. 翻译模型扩展:
   interface TranslationService  
   ├── DeepSeekTranslationServiceImpl
   ├── OpenAITranslationServiceImpl  (新增)
   └── ClaudeTranslationServiceImpl  (新增)

3. 配置化切换:
   spring:
     profiles:
       active: whisper,deepseek  // 可动态切换
```

## 监控和运维

### 关键指标

```
1. 任务处理指标:
   - 任务创建/完成数
   - 平均处理时间
   - 成功/失败率
   - 队列积压情况

2. 服务健康指标:
   - 服务可用性
   - 响应时间
   - 内存使用率
   - CPU使用率

3. 业务指标:
   - 音频识别准确率
   - 翻译质量评分
   - 用户满意度
```

### 告警机制

```
1. 服务异常告警:
   - 服务下线
   - 响应时间超时
   - 错误率过高

2. 资源告警:
   - 内存使用超过阈值
   - 磁盘空间不足
   - 队列积压严重

3. 业务告警:
   - 任务处理失败
   - API调用异常
   - 文件处理错误
```

这种架构设计确保了系统的高可扩展性、高可用性和良好的维护性，同时支持业务的快速迭代和技术栈的灵活切换。