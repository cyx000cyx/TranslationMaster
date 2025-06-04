package com.translation.common.kafka;

/**
 * Kafka主题定义
 */
public class KafkaTopics {
    
    /**
     * 任务创建主题 - task-service发送，speech-service接收
     */
    public static final String TASK_CREATED = "task.created";
    
    /**
     * 语音识别完成主题 - speech-service发送，translate-service接收
     */
    public static final String SPEECH_RECOGNITION_COMPLETED = "speech.recognition.completed";
    
    /**
     * 翻译完成主题 - translate-service发送，encoding-service接收
     */
    public static final String TRANSLATION_COMPLETED = "translation.completed";
    
    /**
     * 编码完成主题 - encoding-service发送，task-service接收（更新任务状态）
     */
    public static final String ENCODING_COMPLETED = "encoding.completed";
    
    /**
     * 任务失败主题 - 任何服务都可以发送
     */
    public static final String TASK_FAILED = "task.failed";
}