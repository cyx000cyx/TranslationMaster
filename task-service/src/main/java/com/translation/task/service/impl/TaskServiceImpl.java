package com.translation.task.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.translation.common.kafka.KafkaTopics;
import com.translation.common.kafka.message.TaskCreatedMessage;
import com.translation.task.dto.CreateTaskRequest;
import com.translation.task.dto.TaskQueryRequest;
import com.translation.task.dto.TaskResponse;
import com.translation.task.entity.TranslationTask;
import com.translation.task.mapper.TranslationTaskMapper;
import com.translation.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务服务实现类 - 系统入口服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    @Resource
    private TranslationTaskMapper translationTaskMapper;
    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${audio.source.path:./audio-source}")
    private String audioSourcePath;
    
    @Override
    @Transactional
    public Map<String, Object> createAudioTranslationTask(CreateTaskRequest request) {
        // 生成任务ID
        String taskId = IdUtil.simpleUUID();
        
        // 验证音频目录
        String audioDirectoryPath = audioSourcePath + "/" + request.getAudioDirectory();
        File audioDir = new File(audioDirectoryPath);
        
        if (!audioDir.exists() || !audioDir.isDirectory()) {
            throw new RuntimeException("音频目录不存在: " + request.getAudioDirectory());
        }
        
        // 统计音频文件数量
        File[] mp3Files = audioDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
        int totalFiles = mp3Files != null ? mp3Files.length : 0;
        
        if (totalFiles == 0) {
            throw new RuntimeException("音频目录中没有MP3文件: " + request.getAudioDirectory());
        }
        
        // 创建任务记录
        TranslationTask task = new TranslationTask();
        task.setTaskId(taskId);
        task.setTaskType(TranslationTask.Type.AUDIO_TRANSLATION);
        task.setAudioDirectoryPath(audioDirectoryPath);
        task.setSourceLanguage(request.getSourceLanguage());
        task.setTargetLanguages(request.getTargetLanguages());
        task.setStatus(TranslationTask.Status.CREATED);
        task.setTotalFiles(totalFiles);
        task.setProcessedFiles(0);
        task.setSuccessFiles(0);
        task.setFailedFiles(0);
        task.setProgressPercent(0.0);
        task.setPriority(request.getPriority());
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        // 保存到数据库
        int result = translationTaskMapper.insert(task);
        if (result <= 0) {
            throw new RuntimeException("保存任务到数据库失败");
        }
        
        log.info("任务创建成功: taskId={}, audioDirectory={}, totalFiles={}", 
                 taskId, request.getAudioDirectory(), totalFiles);
        
        // 发送Kafka消息启动处理流程
        TaskCreatedMessage message = new TaskCreatedMessage();
        message.setTaskId(taskId);
        message.setAudioDirectoryPath(audioDirectoryPath);
        message.setSourceLanguage(request.getSourceLanguage());
        message.setTargetLanguages(request.getTargetLanguages());
        message.setTaskType(TranslationTask.Type.AUDIO_TRANSLATION);
        message.setCreatedTime(LocalDateTime.now());
        message.setPriority(request.getPriority());
        
        try {
            kafkaTemplate.send(KafkaTopics.TASK_CREATED, taskId, message);
            log.info("已发送任务创建消息到Kafka: taskId={}", taskId);
            
            // 更新任务状态为处理中
            updateTaskStatus(taskId, TranslationTask.Status.PROCESSING, null);
            
        } catch (Exception e) {
            log.error("发送Kafka消息失败: taskId=" + taskId, e);
            // 如果Kafka发送失败，回滚任务状态
            updateTaskStatus(taskId, TranslationTask.Status.FAILED, "Kafka消息发送失败: " + e.getMessage());
            throw new RuntimeException("启动任务处理失败: " + e.getMessage());
        }
        
        // 返回任务信息
        Map<String, Object> response = new HashMap<>();
        response.put("taskId", taskId);
        response.put("audioDirectory", request.getAudioDirectory());
        response.put("totalFiles", totalFiles);
        response.put("status", TranslationTask.Status.PROCESSING);
        response.put("createTime", task.getCreateTime());
        
        return response;
    }
    
    @Override
    @Transactional
    public boolean cancelTask(String taskId) {
        TranslationTask task = translationTaskMapper.selectOne(
                new LambdaQueryWrapper<TranslationTask>().eq(TranslationTask::getTaskId, taskId)
        );
        
        if (task == null) {
            return false;
        }
        
        // 只能取消未完成的任务
        if (TranslationTask.Status.COMPLETED.equals(task.getStatus()) ||
            TranslationTask.Status.FAILED.equals(task.getStatus()) ||
            TranslationTask.Status.CANCELLED.equals(task.getStatus())) {
            return false;
        }
        
        // 更新任务状态为已取消
        task.setStatus(TranslationTask.Status.CANCELLED);
        task.setUpdateTime(LocalDateTime.now());
        task.setCompleteTime(LocalDateTime.now());
        
        return translationTaskMapper.updateById(task) > 0;
    }
    
    @Override
    public TaskResponse getTaskById(String taskId) {
        TranslationTask task = translationTaskMapper.selectOne(
                new LambdaQueryWrapper<TranslationTask>().eq(TranslationTask::getTaskId, taskId)
        );
        
        if (task == null) {
            return null;
        }
        
        return convertToTaskResponse(task);
    }
    
    @Override
    public boolean restartTask(String taskId) {
        TranslationTask task = translationTaskMapper.selectOne(
                new LambdaQueryWrapper<TranslationTask>().eq(TranslationTask::getTaskId, taskId)
        );
        
        if (task == null) {
            return false;
        }
        
        // 只能重启失败或取消的任务
        if (!TranslationTask.Status.FAILED.equals(task.getStatus()) &&
            !TranslationTask.Status.CANCELLED.equals(task.getStatus())) {
            return false;
        }
        
        // 重置任务状态
        task.setStatus(TranslationTask.Status.CREATED);
        task.setUpdateTime(LocalDateTime.now());
        task.setStartTime(null);
        task.setCompleteTime(null);
        task.setErrorMessage(null);
        task.setProcessedFiles(0);
        task.setSuccessFiles(0);
        task.setFailedFiles(0);
        task.setProgressPercent(0.0);
        
        boolean updated = translationTaskMapper.updateById(task) > 0;
        
        if (updated) {
            try {
                // 重新发送Kafka消息
                TaskCreatedMessage message = new TaskCreatedMessage();
                message.setTaskId(taskId);
                message.setTaskType(task.getTaskType());
                message.setAudioDirectoryPath(task.getAudioDirectoryPath());
                message.setSourceLanguage(task.getSourceLanguage());
                message.setTargetLanguages(task.getTargetLanguages());
                
                kafkaTemplate.send(KafkaTopics.TASK_CREATED, taskId, message);
                log.info("重新发送任务创建消息到Kafka: taskId={}", taskId);
                
                updateTaskStatus(taskId, TranslationTask.Status.PROCESSING, null);
            } catch (Exception e) {
                log.error("重启任务发送Kafka消息失败: taskId=" + taskId, e);
                updateTaskStatus(taskId, TranslationTask.Status.FAILED, "重启任务失败: " + e.getMessage());
                return false;
            }
        }
        
        return updated;
    }
    
    @Override
    public IPage<TaskResponse> getTaskList(TaskQueryRequest request) {
        Page<TranslationTask> page = new Page<>(request.getCurrent(), request.getSize());

        LambdaQueryWrapper<TranslationTask> queryWrapper = new LambdaQueryWrapper<>();
        
        if (request.getTaskName() != null && !request.getTaskName().trim().isEmpty()) {
            queryWrapper.like(TranslationTask::getTaskId, request.getTaskName());
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            queryWrapper.eq(TranslationTask::getStatus, request.getStatus());
        }
        if (request.getSourceLanguage() != null && !request.getSourceLanguage().trim().isEmpty()) {
            queryWrapper.eq(TranslationTask::getSourceLanguage, request.getSourceLanguage());
        }
        if (request.getTargetLanguage() != null && !request.getTargetLanguage().trim().isEmpty()) {
            queryWrapper.like(TranslationTask::getTargetLanguages, request.getTargetLanguage());
        }
        
        // 排序
        if ("ASC".equalsIgnoreCase(request.getOrderDirection())) {
            queryWrapper.orderByAsc(TranslationTask::getCreateTime);
        } else {
            queryWrapper.orderByDesc(TranslationTask::getCreateTime);
        }
        
        IPage<TranslationTask> taskPage = translationTaskMapper.selectPage(page, queryWrapper);
        
        // 转换为响应对象
        IPage<TaskResponse> responsePage = new Page<>();
        responsePage.setCurrent(taskPage.getCurrent());
        responsePage.setSize(taskPage.getSize());
        responsePage.setTotal(taskPage.getTotal());
        responsePage.setPages(taskPage.getPages());
        
        List<TaskResponse> responseList = taskPage.getRecords().stream()
            .map(this::convertToTaskResponse)
            .collect(Collectors.toList());
        responsePage.setRecords(responseList);
        
        return responsePage;
    }
    
    @Override
    public Map<String, Object> getTaskStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 总任务数
        long totalTasks = translationTaskMapper.selectCount(null);
        statistics.put("totalTasks", totalTasks);
        
        // 各状态任务数
        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("created", translationTaskMapper.selectCount(new QueryWrapper<TranslationTask>().eq("status", TranslationTask.Status.CREATED)));
        statusCounts.put("processing", translationTaskMapper.selectCount(new QueryWrapper<TranslationTask>().eq("status", TranslationTask.Status.PROCESSING)));
        statusCounts.put("completed", translationTaskMapper.selectCount(new QueryWrapper<TranslationTask>().eq("status", TranslationTask.Status.COMPLETED)));
        statusCounts.put("failed", translationTaskMapper.selectCount(new QueryWrapper<TranslationTask>().eq("status", TranslationTask.Status.FAILED)));
        statusCounts.put("cancelled", translationTaskMapper.selectCount(new QueryWrapper<TranslationTask>().eq("status", TranslationTask.Status.CANCELLED)));
        
        statistics.put("statusCounts", statusCounts);
        
        // 今日任务数
        LambdaQueryWrapper<TranslationTask> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(TranslationTask::getCreateTime, LocalDateTime.now().toLocalDate());
        long todayTasks = translationTaskMapper.selectCount(todayWrapper);
        statistics.put("todayTasks", todayTasks);
        
        return statistics;
    }
    
    private TaskResponse convertToTaskResponse(TranslationTask task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTaskName(task.getTaskId());
        response.setAudioFilePath(task.getAudioDirectoryPath());
        response.setSourceLanguage(task.getSourceLanguage());
        response.setTargetLanguage(task.getTargetLanguages());
        response.setStatus(task.getStatus());
        response.setErrorMessage(task.getErrorMessage());
        response.setCreatedAt(task.getCreateTime());
        response.setUpdatedAt(task.getUpdateTime());
        return response;
    }
    
    @Override
    @Transactional
    public void updateTaskStatus(String taskId, String status, String errorMessage) {
        TranslationTask task = translationTaskMapper.selectOne(
            new LambdaQueryWrapper<TranslationTask>().eq(TranslationTask::getTaskId, taskId)
        );
        
        if (task != null) {
            task.setStatus(status);
            task.setUpdateTime(LocalDateTime.now());
            
            if (errorMessage != null) {
                task.setErrorMessage(errorMessage);
            }
            
            if (TranslationTask.Status.PROCESSING.equals(status)) {
                task.setStartTime(LocalDateTime.now());
            } else if (TranslationTask.Status.COMPLETED.equals(status) || 
                       TranslationTask.Status.FAILED.equals(status)) {
                task.setCompleteTime(LocalDateTime.now());
            }
            
            translationTaskMapper.updateById(task);
            log.info("任务状态更新: taskId={}, status={}", taskId, status);
        }
    }
}