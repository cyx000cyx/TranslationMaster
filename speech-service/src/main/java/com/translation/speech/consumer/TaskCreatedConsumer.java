package com.translation.speech.consumer;

import com.translation.common.kafka.KafkaTopics;
import com.translation.common.kafka.consumer.MemoryAwareConsumer;
import com.translation.common.kafka.message.SpeechRecognitionCompletedMessage;
import com.translation.common.kafka.message.TaskCreatedMessage;
import com.translation.speech.service.SpeechRecognitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务创建消息消费者 - 语音识别服务
 * 接收task-service发送的任务创建消息，执行语音识别处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskCreatedConsumer extends MemoryAwareConsumer {

    @Resource
    private SpeechRecognitionService speechRecognitionService;
    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @KafkaListener(topics = KafkaTopics.TASK_CREATED, groupId = "speech-service-group")
    public void handleTaskCreated(@Payload TaskCreatedMessage message,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 @Header(KafkaHeaders.OFFSET) long offset,
                                 Acknowledgment acknowledgment) {
        
        log.info("接收到任务创建消息: taskId={}, topic={}, offset={}",
                 message.getTaskId(), topic, offset);
        
        // 检查内存使用情况
        if (shouldStopConsuming()) {
            log.warn("内存使用率过高，跳过消息处理: taskId={}", message.getTaskId());
            forceGarbageCollection();
            return;
        }
        
        try {
            processSpeechRecognition(message);
            acknowledgment.acknowledge();
            log.info("任务消息处理完成: taskId={}", message.getTaskId());
            
        } catch (Exception e) {
            log.error("处理任务创建消息失败: taskId=" + message.getTaskId(), e);
            sendTaskFailedMessage(message.getTaskId(), "语音识别处理失败: " + e.getMessage());
            acknowledgment.acknowledge();
        }
    }
    
    private void processSpeechRecognition(TaskCreatedMessage message) {
        String taskId = message.getTaskId();
        String audioDirectoryPath = message.getAudioDirectoryPath();
        String sourceLanguage = message.getSourceLanguage();
        
        log.info("开始语音识别处理: taskId={}, audioDirectory={}, sourceLanguage={}", 
                 taskId, audioDirectoryPath, sourceLanguage);
        
        if (!speechRecognitionService.isModelAvailable()) {
            throw new RuntimeException("语音识别模型不可用");
        }
        
        Map<String, Object> options = new HashMap<>();
        options.put("taskId", taskId);
        options.put("priority", message.getPriority());
        
        SpeechRecognitionService.BatchRecognitionResult batchResult = 
            speechRecognitionService.batchRecognizeAudio(audioDirectoryPath, sourceLanguage, options);
        
        if (!batchResult.isSuccess()) {
            throw new RuntimeException("批量语音识别失败: " + batchResult.getErrorMessage());
        }
        
        log.info("语音识别完成: taskId={}, 总文件数={}, 成功={}, 失败={}", 
                 taskId, batchResult.getTotalFiles(), batchResult.getSuccessCount(), batchResult.getFailureCount());
        
        SpeechRecognitionCompletedMessage completedMessage = new SpeechRecognitionCompletedMessage();
        completedMessage.setTaskId(taskId);
        completedMessage.setAudioDirectoryPath(audioDirectoryPath);
        completedMessage.setSourceLanguage(sourceLanguage);
        completedMessage.setTargetLanguages(message.getTargetLanguages());
        completedMessage.setCompletedTime(LocalDateTime.now());
        
        List<SpeechRecognitionCompletedMessage.RecognitionResult> recognitionResults = 
            batchResult.getResults().stream()
                .filter(SpeechRecognitionService.SpeechRecognitionResult::isSuccess)
                .map(result -> new SpeechRecognitionCompletedMessage.RecognitionResult(
                    result.getAudioFileName(),
                    result.getRecognizedText(),
                    result.getConfidence(),
                    getTextFilePath(audioDirectoryPath, result.getAudioFileName())
                ))
                .collect(Collectors.toList());
        
        completedMessage.setRecognitionResults(recognitionResults);
        
        try {
            kafkaTemplate.send(KafkaTopics.SPEECH_RECOGNITION_COMPLETED, taskId, completedMessage);
            log.info("已发送语音识别完成消息: taskId={}", taskId);
            
        } catch (Exception e) {
            log.error("发送语音识别完成消息失败: taskId=" + taskId, e);
            throw new RuntimeException("发送完成消息失败: " + e.getMessage());
        }
    }
    
    private String getTextFilePath(String audioDirectoryPath, String audioFileName) {
        String baseName = audioFileName.replaceFirst("[.][^.]+$", "");
        return audioDirectoryPath + "/" + baseName + "_recognition.txt";
    }
    
    private void sendTaskFailedMessage(String taskId, String errorMessage) {
        try {
            Map<String, Object> failedMessage = new HashMap<>();
            failedMessage.put("taskId", taskId);
            failedMessage.put("service", "speech-service");
            failedMessage.put("errorMessage", errorMessage);
            failedMessage.put("failedTime", LocalDateTime.now());
            
            kafkaTemplate.send(KafkaTopics.TASK_FAILED, taskId, failedMessage);
            log.info("已发送任务失败消息: taskId={}", taskId);
            
        } catch (Exception e) {
            log.error("发送任务失败消息异常: taskId=" + taskId, e);
        }
    }
}