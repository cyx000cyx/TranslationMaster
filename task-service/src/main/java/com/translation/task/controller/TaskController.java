package com.translation.task.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.translation.common.response.ApiResponse;
import com.translation.task.dto.CreateTaskRequest;
import com.translation.task.dto.TaskQueryRequest;
import com.translation.task.dto.TaskResponse;
import com.translation.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * 翻译任务控制器 - 系统入口
 * 提供音频翻译任务的创建、查询、取消等REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Validated
public class TaskController {

    @Resource
    private TaskService taskService;

    /**
     * 创建音频翻译任务（系统入口）
     */
    @PostMapping("/audio/create")
    public ApiResponse<Map<String, Object>> createAudioTranslationTask(@Valid @RequestBody CreateTaskRequest request) {
        
        log.info("收到音频翻译任务创建请求: {}", request);
        
        try {
            Map<String, Object> response = taskService.createAudioTranslationTask(request);
            log.info("音频翻译任务创建成功，任务ID: {}", response.get("taskId"));
            
            return ApiResponse.success(response, "音频翻译任务创建成功");
        } catch (Exception e) {
            log.error("创建音频翻译任务失败", e);
            return ApiResponse.error("创建任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务详情
     */
    @GetMapping("/{taskId}")
    public ApiResponse<TaskResponse> getTask(@PathVariable String taskId) {
        log.info("查询任务详情，任务ID: {}", taskId);
        
        try {
            TaskResponse response = taskService.getTaskById(taskId);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("查询任务失败，任务ID: {}", taskId, e);
            return ApiResponse.error("查询任务失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询任务列表
     */
    @PostMapping("/list")
    public ApiResponse<IPage<TaskResponse>> getTasks(@Valid @RequestBody TaskQueryRequest request) {
        log.info("分页查询任务列表: {}", request);
        
        try {
            IPage<TaskResponse> response = taskService.getTaskList(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("查询任务列表失败", e);
            return ApiResponse.error("查询任务列表失败: " + e.getMessage());
        }
    }

    /**
     * 取消任务
     */
    @PostMapping("/{taskId}/cancel")
    public ApiResponse<Void> cancelTask(@PathVariable String taskId) {
        log.info("取消任务，任务ID: {}", taskId);
        
        try {
            taskService.cancelTask(taskId);
            return ApiResponse.success(null, "任务取消成功");
        } catch (Exception e) {
            log.error("取消任务失败，任务ID: {}", taskId, e);
            return ApiResponse.error("取消任务失败: " + e.getMessage());
        }
    }

    /**
     * 重启任务
     * 用于故障转移和任务重做
     */
    @PostMapping("/{taskId}/restart")
    public ApiResponse<Void> restartTask(@PathVariable String taskId) {
        log.info("重启任务，任务ID: {}", taskId);
        
        try {
            taskService.restartTask(taskId);
            return ApiResponse.success(null, "任务重启成功");
        } catch (Exception e) {
            log.error("重启任务失败，任务ID: {}", taskId, e);
            return ApiResponse.error("重启任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务统计信息
     */
    @GetMapping("/statistics")
    public ApiResponse<Object> getTaskStatistics() {
        log.info("获取任务统计信息");
        
        try {
            Object statistics = taskService.getTaskStatistics();
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取任务统计信息失败", e);
            return ApiResponse.error("获取统计信息失败: " + e.getMessage());
        }
    }
}
