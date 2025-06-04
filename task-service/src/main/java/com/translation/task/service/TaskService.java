package com.translation.task.service;

import com.translation.task.dto.CreateTaskRequest;
import com.translation.task.dto.TaskResponse;
import com.translation.task.dto.TaskQueryRequest;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

/**
 * 任务服务接口
 */
public interface TaskService {
    
    /**
     * 创建音频翻译任务（系统入口）
     */
    Map<String, Object> createAudioTranslationTask(CreateTaskRequest request);
    
    /**
     * 根据ID获取任务详情
     */
    TaskResponse getTaskById(String taskId);
    
    /**
     * 取消任务
     */
    boolean cancelTask(String taskId);
    
    /**
     * 重启任务
     */
    boolean restartTask(String taskId);
    
    /**
     * 获取任务列表（分页查询）
     */
    IPage<TaskResponse> getTaskList(TaskQueryRequest request);
    
    /**
     * 获取任务统计信息
     */
    Map<String, Object> getTaskStatistics();
    
    /**
     * 更新任务状态
     */
    void updateTaskStatus(String taskId, String status, String errorMessage);
}