package com.translation.translate.service;

import lombok.Data;

import java.util.Map;

/**
 * 翻译服务接口
 * 抽象化翻译实现，支持多种翻译模型（DeepSeek、GPT、其他LLM）
 */
public interface TranslationService {
    
    /**
     * 翻译单个文本
     * 
     * @param sourceText 源文本
     * @param sourceLanguage 源语言代码
     * @param targetLanguage 目标语言代码
     * @param options 额外选项参数
     * @return 翻译结果
     */
    TranslationResult translateText(String sourceText, String sourceLanguage, String targetLanguage, Map<String, Object> options);
    
    /**
     * 批量翻译文本到多种语言
     * 
     * @param sourceText 源文本
     * @param sourceLanguage 源语言代码
     * @param targetLanguages 目标语言代码数组
     * @param options 额外选项参数
     * @return 批量翻译结果
     */
    BatchTranslationResult batchTranslateText(String sourceText, String sourceLanguage, String[] targetLanguages, Map<String, Object> options);
    
    /**
     * 批量翻译多个文本到多种语言
     * 
     * @param sourceTexts 源文本映射（文件名 -> 文本内容）
     * @param sourceLanguage 源语言代码
     * @param targetLanguages 目标语言代码数组
     * @param options 额外选项参数
     * @return 批量翻译结果
     */
    MultiBatchTranslationResult multiBatchTranslateTexts(Map<String, String> sourceTexts, String sourceLanguage, String[] targetLanguages, Map<String, Object> options);
    
    /**
     * 检查翻译服务是否可用
     */
    boolean isServiceAvailable();
    
    /**
     * 单个翻译结果
     */
    @Data
    class TranslationResult {
        private boolean success;
        private String sourceText;
        private String translatedText;
        private String sourceLanguage;
        private String targetLanguage;
        private String errorMessage;
        private Long processingTimeMs;
        private Double confidence;
        
        public TranslationResult(boolean success, String sourceText, String translatedText, 
                               String sourceLanguage, String targetLanguage, String errorMessage, 
                               Long processingTimeMs, Double confidence) {
            this.success = success;
            this.sourceText = sourceText;
            this.translatedText = translatedText;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.errorMessage = errorMessage;
            this.processingTimeMs = processingTimeMs;
            this.confidence = confidence;
        }
    }
    
    /**
     * 批量翻译结果（单个文本到多种语言）
     */
    @Data
    class BatchTranslationResult {
        private boolean success;
        private String sourceText;
        private String sourceLanguage;
        private Map<String, TranslationResult> translations; // 目标语言 -> 翻译结果
        private int totalLanguages;
        private int successCount;
        private int failureCount;
        private String errorMessage;
        
        public BatchTranslationResult(boolean success, String sourceText, String sourceLanguage, 
                                    Map<String, TranslationResult> translations, int totalLanguages, 
                                    int successCount, int failureCount, String errorMessage) {
            this.success = success;
            this.sourceText = sourceText;
            this.sourceLanguage = sourceLanguage;
            this.translations = translations;
            this.totalLanguages = totalLanguages;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.errorMessage = errorMessage;
        }
    }
    
    /**
     * 多文本批量翻译结果
     */
    @Data
    class MultiBatchTranslationResult {
        private boolean success;
        private String sourceLanguage;
        private String[] targetLanguages;
        private Map<String, BatchTranslationResult> results; // 文件名 -> 批量翻译结果
        private int totalFiles;
        private int successFiles;
        private int failureFiles;
        private String errorMessage;
        
        public MultiBatchTranslationResult(boolean success, String sourceLanguage, String[] targetLanguages,
                                         Map<String, BatchTranslationResult> results, int totalFiles,
                                         int successFiles, int failureFiles, String errorMessage) {
            this.success = success;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguages = targetLanguages;
            this.results = results;
            this.totalFiles = totalFiles;
            this.successFiles = successFiles;
            this.failureFiles = failureFiles;
            this.errorMessage = errorMessage;
        }
    }
}