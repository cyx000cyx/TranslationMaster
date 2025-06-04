package com.translation.task.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建任务请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {
    
    /**
     * 音频文件目录名称（相对于audio-source目录）
     */
    @NotBlank(message = "音频文件目录不能为空")
    private String audioDirectory;
    
    /**
     * 源语言代码
     */
    @NotBlank(message = "源语言不能为空")
    private String sourceLanguage;
    
    /**
     * 目标语言列表（逗号分隔）
     */
    @NotBlank(message = "目标语言不能为空")
    private String targetLanguages;
    
    /**
     * 任务优先级（1-10，数字越小优先级越高）
     */
    private Integer priority = 5;
    
    /**
     * 任务描述
     */
    private String description;
}