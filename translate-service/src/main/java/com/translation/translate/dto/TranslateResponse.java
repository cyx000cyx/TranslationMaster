package com.translation.translate.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 翻译响应DTO
 * 用于返回翻译结果和相关信息
 */
@Data
public class TranslateResponse {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 源文本
     */
    private String sourceText;

    /**
     * 检测到的源语言
     */
    private String sourceLanguage;

    /**
     * 翻译结果映射（语言代码 -> 翻译文本）
     */
    private Map<String, String> translations;

    /**
     * 批量翻译结果（用于批量翻译）
     */
    private List<Map<String, Object>> batchResults;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息列表
     */
    private List<String> errors;

    /**
     * 处理耗时（毫秒）
     */
    private Long processingTime;

    /**
     * 翻译质量评分
     */
    private Map<String, Double> qualityScores;

    /**
     * 置信度评估
     */
    private Map<String, Double> confidenceScores;

    /**
     * 翻译统计信息
     */
    private TranslationStats stats;

    /**
     * 使用的翻译模型信息
     */
    private ModelInfo modelInfo;

    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 缓存信息
     */
    private CacheInfo cacheInfo;

    /**
     * 翻译统计信息
     */
    @Data
    public static class TranslationStats {
        /**
         * 源文本字符数
         */
        private Integer sourceCharCount;
        
        /**
         * 源文本词数
         */
        private Integer sourceWordCount;
        
        /**
         * 翻译文本统计（按语言）
         */
        private Map<String, CharWordCount> targetStats;
        
        /**
         * 平均翻译速度（字符/秒）
         */
        private Double avgTranslationSpeed;
    }

    /**
     * 字符词数统计
     */
    @Data
    public static class CharWordCount {
        private Integer charCount;
        private Integer wordCount;
    }

    /**
     * 模型信息
     */
    @Data
    public static class ModelInfo {
        /**
         * 使用的翻译模型
         */
        private String modelName;
        
        /**
         * 模型版本
         */
        private String modelVersion;
        
        /**
         * API提供商
         */
        private String provider;
        
        /**
         * 支持的功能特性
         */
        private List<String> features;
    }

    /**
     * 缓存信息
     */
    @Data
    public static class CacheInfo {
        /**
         * 是否命中缓存
         */
        private Boolean hitCache;
        
        /**
         * 缓存键
         */
        private String cacheKey;
        
        /**
         * 缓存过期时间
         */
        private LocalDateTime expireTime;
    }
}
