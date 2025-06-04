package com.translation.task.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 任务查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskQueryRequest {
    
    /**
     * 任务名称（模糊查询）
     */
    private String taskName;
    
    /**
     * 任务状态
     */
    private String status;
    
    /**
     * 源语言
     */
    private String sourceLanguage;
    
    /**
     * 目标语言
     */
    private String targetLanguage;
    
    /**
     * 当前页码
     */
    private Integer current = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 排序字段
     */
    private String orderBy = "created_at";
    
    /**
     * 排序方式（ASC/DESC）
     */
    private String orderDirection = "DESC";
}