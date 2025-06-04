package com.translation.speech.service;

import lombok.Data;

import java.util.Map;

/**
 * 语音识别服务接口
 * 抽象化语音识别实现，支持多种模型（Whisper、其他ASR模型）
 */
public interface SpeechRecognitionService {
    
    /**
     * 识别音频文件
     * 
     * @param audioFilePath 音频文件路径
     * @param language 语言代码
     * @param options 额外选项参数
     * @return 识别结果
     */
    SpeechRecognitionResult recognizeAudio(String audioFilePath, String language, Map<String, Object> options);
    
    /**
     * 批量识别音频文件
     * 
     * @param audioDirectoryPath 音频目录路径
     * @param language 语言代码
     * @param options 额外选项参数
     * @return 批量识别结果
     */
    BatchRecognitionResult batchRecognizeAudio(String audioDirectoryPath, String language, Map<String, Object> options);
    
    /**
     * 检查模型是否可用
     */
    boolean isModelAvailable();
    
    /**
     * 语音识别结果
     */
    @Data
    class SpeechRecognitionResult {
        private boolean success;
        private String recognizedText;
        private Double confidence;
        private String audioFileName;
        private String errorMessage;
        private Long processingTimeMs;
        
        // 构造函数
        public SpeechRecognitionResult(boolean success, String recognizedText, Double confidence, 
                                     String audioFileName, String errorMessage, Long processingTimeMs) {
            this.success = success;
            this.recognizedText = recognizedText;
            this.confidence = confidence;
            this.audioFileName = audioFileName;
            this.errorMessage = errorMessage;
            this.processingTimeMs = processingTimeMs;
        }
    }
    
    /**
     * 批量识别结果
     */
    @Data
    class BatchRecognitionResult {
        private boolean success;
        private java.util.List<SpeechRecognitionResult> results;
        private int totalFiles;
        private int successCount;
        private int failureCount;
        private String errorMessage;
        
        // 构造函数
        public BatchRecognitionResult(boolean success, java.util.List<SpeechRecognitionResult> results, 
                                    int totalFiles, int successCount, int failureCount, String errorMessage) {
            this.success = success;
            this.results = results;
            this.totalFiles = totalFiles;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.errorMessage = errorMessage;
        }
    }
}