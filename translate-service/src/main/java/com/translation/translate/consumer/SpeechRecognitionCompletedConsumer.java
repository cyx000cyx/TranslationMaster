package com.translation.translate.consumer;

import com.translation.common.kafka.KafkaTopics;
import com.translation.common.kafka.consumer.MemoryAwareConsumer;
import com.translation.common.kafka.message.SpeechRecognitionCompletedMessage;
import com.translation.common.kafka.message.TranslationCompletedMessage;
import com.translation.translate.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 语音识别完成消息消费者 - 翻译服务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpeechRecognitionCompletedConsumer extends MemoryAwareConsumer {

    @Resource
    private TranslationService translationService;
    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @KafkaListener(topics = KafkaTopics.SPEECH_RECOGNITION_COMPLETED, groupId = "translate-service-group")
    public void handleSpeechRecognitionCompleted(@Payload SpeechRecognitionCompletedMessage message,
                                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                                @Header(KafkaHeaders.OFFSET) long offset,
                                                Acknowledgment acknowledgment) {
        
        log.info("接收到语音识别完成消息: taskId={}, topic={}, offset={}",
                 message.getTaskId(), topic, offset);
        
        if (shouldStopConsuming()) {
            log.warn("内存使用率过高，跳过消息处理: taskId={}", message.getTaskId());
            forceGarbageCollection();
            return;
        }
        
        try {
            processTranslation(message);
            acknowledgment.acknowledge();
            log.info("翻译任务处理完成: taskId={}", message.getTaskId());
            
        } catch (Exception e) {
            log.error("处理语音识别完成消息失败: taskId=" + message.getTaskId(), e);
            sendTaskFailedMessage(message.getTaskId(), "翻译处理失败: " + e.getMessage());
            acknowledgment.acknowledge();
        }
    }
    
    private void processTranslation(SpeechRecognitionCompletedMessage message) {
        String taskId = message.getTaskId();
        String audioDirectoryPath = message.getAudioDirectoryPath();
        String sourceLanguage = message.getSourceLanguage();
        String[] targetLanguagesArr = message.getTargetLanguages().split(",");
        
        log.info("开始翻译处理: taskId={}, sourceLanguage={}, targetLanguagesArr={}",
                 taskId, sourceLanguage, Arrays.toString(targetLanguagesArr));

        Map<String, String> sourceTextsMap = getSourceTextsMap(message);

        Map<String, Object> options = new HashMap<>();
        options.put("taskId", taskId);
        
        TranslationService.MultiBatchTranslationResult batchResult = 
            translationService.multiBatchTranslateTexts(sourceTextsMap, sourceLanguage, targetLanguagesArr, options);
        
        if (!batchResult.isSuccess()) {
            throw new RuntimeException("批量翻译失败: " + batchResult.getErrorMessage());
        }
        
        log.info("翻译完成: taskId={}, 总文件数={}, 成功={}, 失败={}", 
                 taskId, batchResult.getTotalFiles(), batchResult.getSuccessFiles(), batchResult.getFailureFiles());
        
        List<TranslationCompletedMessage.TranslationResult> translationResults = 
            saveTranslationResults(audioDirectoryPath, batchResult);
        
        TranslationCompletedMessage completedMessage = new TranslationCompletedMessage();
        completedMessage.setTaskId(taskId);
        completedMessage.setAudioDirectoryPath(audioDirectoryPath);
        completedMessage.setTranslationResults(translationResults);
        completedMessage.setCompletedTime(LocalDateTime.now());
        
        try {
            kafkaTemplate.send(KafkaTopics.TRANSLATION_COMPLETED, taskId, completedMessage);
            log.info("已发送翻译完成消息: taskId={}", taskId);
        } catch (Exception e) {
            log.error("发送翻译完成消息失败! taskId=" + taskId, e);
            throw new RuntimeException("发送完成消息失败: " + e.getMessage());
        }
    }

    @NotNull
    private Map<String, String> getSourceTextsMap(SpeechRecognitionCompletedMessage message) {
        boolean serviceAvailable = translationService.isServiceAvailable();
        if (!serviceAvailable) {
            throw new RuntimeException("翻译服务不可用，请检查API配置");
        }

        List<SpeechRecognitionCompletedMessage.RecognitionResult> recognitionResults = message.getRecognitionResults();
        if (recognitionResults == null || recognitionResults.isEmpty()) {
            throw new RuntimeException("没有可翻译的语音识别结果");
        }

        return getSourceTextsMapFromRecognition(recognitionResults);
    }

    @NotNull
    private static Map<String, String> getSourceTextsMapFromRecognition(
            List<SpeechRecognitionCompletedMessage.RecognitionResult> recognitionResults) {
        Map<String, String> sourceTexts = new HashMap<>();
        for (SpeechRecognitionCompletedMessage.RecognitionResult result : recognitionResults) {
            String fileName = result.getAudioFileName();
            String recognizedText = result.getRecognizedText();

            if (recognizedText != null && !recognizedText.trim().isEmpty()) {
                sourceTexts.put(fileName, recognizedText);
            }
        }

        if (sourceTexts.isEmpty()) {
            throw new RuntimeException("所有语音识别结果都为空，无法进行翻译");
        }
        return sourceTexts;
    }

    private List<TranslationCompletedMessage.TranslationResult> saveTranslationResults(
            String audioDirectoryPath, TranslationService.MultiBatchTranslationResult batchResult) {
        
        List<TranslationCompletedMessage.TranslationResult> results = new ArrayList<>();
        
        for (Map.Entry<String, TranslationService.BatchTranslationResult> entry : batchResult.getResults().entrySet()) {
            String audioFileName = entry.getKey();
            TranslationService.BatchTranslationResult result = entry.getValue();
            
            if (!result.isSuccess()) {
                log.warn("跳过翻译失败的文件: {}", audioFileName);
                continue;
            }
            
            try {
                Map<String, String> translations = new HashMap<>();
                for (Map.Entry<String, TranslationService.TranslationResult> translationEntry : result.getTranslations().entrySet()) {
                    String targetLanguage = translationEntry.getKey();
                    TranslationService.TranslationResult translationResult = translationEntry.getValue();
                    
                    if (translationResult.isSuccess()) {
                        translations.put(targetLanguage, translationResult.getTranslatedText());
                    }
                }
                
                String baseName = audioFileName.replaceFirst("[.][^.]+$", "");
                String translationFilePath = audioDirectoryPath + "/" + baseName + "_translations.json";
                
                Map<String, Object> translationData = new HashMap<>();
                translationData.put("audioFileName", audioFileName);
                translationData.put("originalText", result.getSourceText());
                translationData.put("sourceLanguage", result.getSourceLanguage());
                translationData.put("translations", translations);
                translationData.put("translationTime", LocalDateTime.now());
                
                Files.write(Paths.get(translationFilePath), 
                           cn.hutool.json.JSONUtil.toJsonPrettyStr(translationData).getBytes("UTF-8"));
                
                log.debug("翻译结果已保存: {}", translationFilePath);
                
                TranslationCompletedMessage.TranslationResult translationResult = 
                    new TranslationCompletedMessage.TranslationResult(
                        audioFileName, result.getSourceText(), translations, translationFilePath);
                
                results.add(translationResult);
                
            } catch (Exception e) {
                log.error("保存翻译结果失败: " + audioFileName, e);
            }
        }
        
        return results;
    }
    
    private void sendTaskFailedMessage(String taskId, String errorMessage) {
        try {
            Map<String, Object> failedMessage = new HashMap<>();
            failedMessage.put("taskId", taskId);
            failedMessage.put("service", "translate-service");
            failedMessage.put("errorMessage", errorMessage);
            failedMessage.put("failedTime", LocalDateTime.now());
            
            kafkaTemplate.send(KafkaTopics.TASK_FAILED, taskId, failedMessage);
            log.info("已发送任务失败消息: taskId={}", taskId);
            
        } catch (Exception e) {
            log.error("发送任务失败消息异常: taskId=" + taskId, e);
        }
    }
}