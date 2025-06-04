package com.translation.common.kafka.message;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 语音识别完成消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeechRecognitionCompletedMessage {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 音频文件目录路径
     */
    private String audioDirectoryPath;
    
    /**
     * 识别结果文件路径列表
     */
    private List<RecognitionResult> recognitionResults;
    
    /**
     * 源语言
     */
    private String sourceLanguage;
    
    /**
     * 目标语言列表
     */
    private String targetLanguages;
    
    /**
     * 处理完成时间
     */
    private LocalDateTime completedTime;
    
    /**
     * 识别结果详情
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecognitionResult {
        /**
         * 音频文件名
         */
        private String audioFileName;
        
        /**
         * 识别文本
         */
        private String recognizedText;
        
        /**
         * 置信度
         */
        private Double confidence;
        
        /**
         * 文本结果文件路径
         */
        private String textFilePath;
    }
}