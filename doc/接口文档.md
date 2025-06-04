# API 接口文档

## 概述

多语言音频翻译系统提供RESTful API接口，支持音频翻译任务的创建、查询和管理。系统采用统一的响应格式和错误处理机制。

## 基础信息

- **基础URL**: `http://localhost:8001`
- **协议**: HTTP/HTTPS
- **数据格式**: JSON
- **字符编码**: UTF-8

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2024-01-15T10:30:00"
}
```

### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 200    | 请求成功 |
| 400    | 请求参数错误 |
| 401    | 未授权访问 |
| 404    | 资源不存在 |
| 500    | 服务器内部错误 |

## API 接口详情

### 1. 创建音频翻译任务

**接口地址**: `POST /api/tasks/audio/create`

**接口描述**: 创建新的音频翻译任务，系统将异步处理音频文件的识别和翻译。

**请求参数**:
```json
{
  "audioDirectory": "Tilly's Lost Balloon",
  "sourceLanguage": "en",
  "targetLanguages": "zh-CN,ja,ko",
  "priority": 5,
  "description": "英语童话故事翻译"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| audioDirectory | String | 是 | 音频文件目录名称（相对于audio-source目录） |
| sourceLanguage | String | 是 | 源语言代码（如：en, zh, ja） |
| targetLanguages | String | 是 | 目标语言列表，逗号分隔（如：zh-CN,ja,ko） |
| priority | Integer | 否 | 任务优先级，1-10，数字越小优先级越高，默认5 |
| description | String | 否 | 任务描述信息 |

**支持的语言代码**:

| 代码 | 语言名称 |
|------|----------|
| en | 英语 |
| zh-CN | 简体中文 |
| zh-TW | 繁体中文 |
| ja | 日语 |

**响应示例**:
```json
{
  "code": 200,
  "message": "音频翻译任务创建成功",
  "data": {
    "taskId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "audioDirectory": "Tilly's Lost Balloon",
    "totalFiles": 8,
    "status": "PROCESSING",
    "createTime": "2024-01-15T10:30:00"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

**错误响应示例**:
```json
{
  "code": 400,
  "message": "音频目录不存在: invalid_folder",
  "data": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

### 2. 查询任务状态

**接口地址**: `GET /api/tasks/{taskId}`

**接口描述**: 根据任务ID查询任务的详细状态和处理进度。

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| taskId | String | 任务唯一标识符 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "taskType": "AUDIO_TRANSLATION",
    "audioDirectoryPath": "/opt/audio-source/Tilly's Lost Balloon",
    "sourceLanguage": "en",
    "targetLanguages": "zh-CN,ja,ko",
    "status": "TRANSLATION",
    "totalFiles": 8,
    "processedFiles": 5,
    "successFiles": 5,
    "failedFiles": 0,
    "progressPercent": 62.5,
    "errorMessage": null,
    "createTime": "2024-01-15T10:30:00",
    "updateTime": "2024-01-15T10:35:00",
    "startTime": "2024-01-15T10:30:05",
    "completeTime": null
  },
  "timestamp": "2024-01-15T10:35:00"
}
```

**任务状态说明**:

| 状态 | 说明 |
|------|------|
| CREATED | 任务已创建 |
| PROCESSING | 正在处理中 |
| SPEECH_RECOGNITION | 语音识别阶段 |
| TRANSLATION | 翻译阶段 |
| ENCODING | 编码压缩阶段 |
| COMPLETED | 处理完成 |
| FAILED | 处理失败 |
| CANCELLED | 任务已取消 |

### 3. 取消任务

**接口地址**: `POST /api/tasks/{taskId}/cancel`

**接口描述**: 取消正在处理中的任务。

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| taskId | String | 任务唯一标识符 |

**响应示例**:
```json
{
  "code": 200,
  "message": "任务取消成功",
  "data": "任务取消成功",
  "timestamp": "2024-01-15T10:35:00"
}
```

**错误响应示例**:
```json
{
  "code": 400,
  "message": "任务取消失败，任务可能已完成或不存在",
  "data": null,
  "timestamp": "2024-01-15T10:35:00"
}
```

### 4. 获取任务列表

**接口地址**: `GET /api/tasks/list`

**接口描述**: 分页查询任务列表，支持按状态筛选。

**查询参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | String | 否 | 任务状态筛选 |
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页大小，默认10 |

**请求示例**:
```
GET /api/tasks/list?status=COMPLETED&page=1&size=20
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "taskId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "taskType": "AUDIO_TRANSLATION",
      "audioDirectoryPath": "/opt/audio-source/Tilly's Lost Balloon",
      "sourceLanguage": "en",
      "targetLanguages": "zh-CN,ja,ko",
      "status": "COMPLETED",
      "progressPercent": 100.0,
      "createTime": "2024-01-15T10:30:00",
      "updateTime": "2024-01-15T10:45:00"
    }
  ],
  "timestamp": "2024-01-15T11:00:00"
}
```

### 5. 获取可用音频文件夹

**接口地址**: `GET /api/tasks/audio/folders`

**接口描述**: 获取系统中可用的音频文件夹列表。

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "folderName": "Tilly's Lost Balloon",
      "mp3Count": 8,
      "lastModified": 1705308600000,
      "path": "/opt/audio-source/Tilly's Lost Balloon"
    },
    {
      "folderName": "English Stories",
      "mp3Count": 12,
      "lastModified": 1705222200000,
      "path": "/opt/audio-source/English Stories"
    }
  ],
  "timestamp": "2024-01-15T11:00:00"
}
```

## 使用示例

### cURL命令示例

#### 创建任务
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

#### 查询任务状态
```bash
curl http://localhost:8001/api/tasks/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

#### 获取任务列表
```bash
curl "http://localhost:8001/api/tasks/list?status=COMPLETED&page=1&size=10"
```

#### 获取音频文件夹
```bash
curl http://localhost:8001/api/tasks/audio/folders
```

### JavaScript示例

```javascript
// 创建翻译任务
async function createTranslationTask() {
  const response = await fetch('http://localhost:8001/api/tasks/audio/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      audioDirectory: "Tilly's Lost Balloon",
      sourceLanguage: 'en',
      targetLanguages: 'zh-CN,ja,ko',
      priority: 5
    })
  });
  
  const result = await response.json();
  if (result.code === 200) {
    console.log('任务创建成功:', result.data.taskId);
    return result.data.taskId;
  } else {
    console.error('任务创建失败:', result.message);
  }
}

// 轮询查询任务状态
async function pollTaskStatus(taskId) {
  const response = await fetch(`http://localhost:8001/api/tasks/${taskId}`);
  const result = await response.json();
  
  if (result.code === 200) {
    const task = result.data;
    console.log(`任务状态: ${task.status}, 进度: ${task.progressPercent}%`);
    
    if (task.status === 'COMPLETED') {
      console.log('任务完成!');
      return true;
    } else if (task.status === 'FAILED') {
      console.error('任务失败:', task.errorMessage);
      return false;
    }
  }
  
  return false;
}

// 使用示例
async function main() {
  const taskId = await createTranslationTask();
  if (taskId) {
    // 每5秒检查一次任务状态
    const interval = setInterval(async () => {
      const isCompleted = await pollTaskStatus(taskId);
      if (isCompleted) {
        clearInterval(interval);
      }
    }, 5000);
  }
}
```

### Python示例

```python
import requests
import time
import json

class TranslationClient:
    def __init__(self, base_url="http://localhost:8001"):
        self.base_url = base_url
    
    def create_task(self, audio_directory, source_language, target_languages, priority=5):
        """创建翻译任务"""
        url = f"{self.base_url}/api/tasks/audio/create"
        data = {
            "audioDirectory": audio_directory,
            "sourceLanguage": source_language,
            "targetLanguages": target_languages,
            "priority": priority
        }
        
        response = requests.post(url, json=data)
        result = response.json()
        
        if result["code"] == 200:
            print(f"任务创建成功: {result['data']['taskId']}")
            return result["data"]["taskId"]
        else:
            print(f"任务创建失败: {result['message']}")
            return None
    
    def get_task_status(self, task_id):
        """查询任务状态"""
        url = f"{self.base_url}/api/tasks/{task_id}"
        response = requests.get(url)
        result = response.json()
        
        if result["code"] == 200:
            return result["data"]
        else:
            print(f"查询失败: {result['message']}")
            return None
    
    def wait_for_completion(self, task_id, poll_interval=5):
        """等待任务完成"""
        print(f"等待任务 {task_id} 完成...")
        
        while True:
            task = self.get_task_status(task_id)
            if task is None:
                break
                
            status = task["status"]
            progress = task["progressPercent"]
            
            print(f"任务状态: {status}, 进度: {progress}%")
            
            if status == "COMPLETED":
                print("任务完成!")
                return True
            elif status == "FAILED":
                print(f"任务失败: {task.get('errorMessage', '未知错误')}")
                return False
            
            time.sleep(poll_interval)
        
        return False

# 使用示例
if __name__ == "__main__":
    client = TranslationClient()
    
    # 创建任务
    task_id = client.create_task(
        audio_directory="Tilly's Lost Balloon",
        source_language="en",
        target_languages="zh-CN,ja,ko"
    )
    
    # 等待完成
    if task_id:
        client.wait_for_completion(task_id)
```

## 错误处理

### 常见错误码

| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 400 | 音频目录不存在 | 检查audioDirectory参数是否正确 |
| 400 | 音频目录中没有MP3文件 | 确保目录包含.mp3格式的音频文件 |
| 400 | 源语言不能为空 | 提供有效的sourceLanguage参数 |
| 400 | 目标语言不能为空 | 提供有效的targetLanguages参数 |
| 404 | 任务不存在 | 检查taskId是否正确 |
| 500 | Kafka消息发送失败 | 检查Kafka服务是否正常运行 |
| 500 | 保存任务到数据库失败 | 检查数据库连接和权限 |

### 重试机制

对于临时性错误（如网络超时、服务暂时不可用），建议实现指数退避重试：

```javascript
async function retryRequest(requestFn, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await requestFn();
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      
      const delay = Math.pow(2, i) * 1000; // 指数退避
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
}
```

## 性能指标

### 接口性能

| 接口 | 平均响应时间 | 并发处理能力 |
|------|--------------|--------------|
| 创建任务 | < 200ms | 100 QPS |
| 查询状态 | < 50ms | 500 QPS |
| 任务列表 | < 100ms | 200 QPS |
| 文件夹列表 | < 100ms | 200 QPS |

### 任务处理性能

| 指标 | 数值 |
|------|------|
| 音频识别速度 | 约为音频时长的0.1-0.3倍 |
| 翻译处理速度 | 1000字符/秒 |
| 系统并发任务数 | 10-50个（取决于硬件配置） |

这些API接口为系统提供了完整的音频翻译任务管理功能，支持高并发和大规模音频文件的处理需求。