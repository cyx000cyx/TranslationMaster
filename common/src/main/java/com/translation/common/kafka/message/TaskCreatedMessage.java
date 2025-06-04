package com.translation.common.kafka.message;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务创建消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreatedMessage {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 音频文件目录路径
     */
    private String audioDirectoryPath;
    
    /**
     * 源语言
     */
    private String sourceLanguage;
    
    /**
     * 目标语言列表（逗号分隔）
     */
    private String targetLanguages;
    
    /**
     * 任务类型
     */
    private String taskType;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 优先级（1-10，数字越小优先级越高）
     */
    private Integer priority = 5;
}