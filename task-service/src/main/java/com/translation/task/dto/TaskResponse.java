package com.translation.task.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    
    /**
     * 任务ID
     */
    private Long id;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 音频文件路径
     */
    private String audioFilePath;
    
    /**
     * 源语言
     */
    private String sourceLanguage;
    
    /**
     * 目标语言
     */
    private String targetLanguage;
    
    /**
     * 任务状态
     */
    private String status;
    
    /**
     * 识别结果
     */
    private String recognitionResult;
    
    /**
     * 翻译结果
     */
    private String translationResult;
    
    /**
     * 编码结果
     */
    private String encodedResult;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}