package com.translation.translate.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文本校验响应DTO
 * 返回STT识别结果与原始文本的准确性验证结果
 */
@Data
public class ValidationResponse {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 整体相似度分数（0.0-1.0）
     */
    private Double similarityScore;

    /**
     * 准确性等级
     * EXCELLENT - 优秀 (≥0.95)
     * GOOD - 良好 (≥0.85)
     * FAIR - 一般 (≥0.7)
     * POOR - 差 (≥0.5)
     * VERY_POOR - 很差 (<0.5)
     */
    private String accuracyLevel;

    /**
     * 验证是否通过
     */
    private Boolean validationPassed;

    /**
     * 语言一致性检查
     */
    private Boolean languageConsistent;

    /**
     * 检测到的原始文本语言
     */
    private String originalLanguage;

    /**
     * 检测到的识别文本语言
     */
    private String recognizedLanguage;

    /**
     * 详细相似度分析
     */
    private SimilarityAnalysis similarityAnalysis;

    /**
     * 纠错建议
     */
    private Object correctSuggestions;

    /**
     * 关键词验证结果
     */
    private List<KeywordValidation> keywordValidations;

    /**
     * 详细分析结果
     */
    private Map<String, Object> detailedAnalysis;

    /**
     * 质量评估
     */
    private QualityAssessment qualityAssessment;

    /**
     * 处理耗时（毫秒）
     */
    private Long processingTime;

    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 相似度分析详情
     */
    @Data
    public static class SimilarityAnalysis {
        /**
         * 编辑距离相似度
         */
        private Double levenshteinSimilarity;
        
        /**
         * Jaccard相似度
         */
        private Double jaccardSimilarity;
        
        /**
         * 余弦相似度
         */
        private Double cosineSimilarity;
        
        /**
         * 字符级别相似度
         */
        private Double characterSimilarity;
        
        /**
         * 词级别相似度
         */
        private Double wordSimilarity;
        
        /**
         * 语义相似度
         */
        private Double semanticSimilarity;
    }

    /**
     * 关键词验证结果
     */
    @Data
    public static class KeywordValidation {
        /**
         * 关键词
         */
        private String keyword;
        
        /**
         * 是否在原始文本中找到
         */
        private Boolean foundInOriginal;
        
        /**
         * 是否在识别文本中找到
         */
        private Boolean foundInRecognized;
        
        /**
         * 匹配度
         */
        private Double matchScore;
        
        /**
         * 验证通过
         */
        private Boolean passed;
        
        /**
         * 备注
         */
        private String remarks;
    }

    /**
     * 质量评估
     */
    @Data
    public static class QualityAssessment {
        /**
         * 整体质量分数（0-100）
         */
        private Integer overallQualityScore;
        
        /**
         * 语法正确性
         */
        private Double grammarCorrectness;
        
        /**
         * 词汇准确性
         */
        private Double vocabularyAccuracy;
        
        /**
         * 结构完整性
         */
        private Double structuralIntegrity;
        
        /**
         * 内容完整性
         */
        private Double contentCompleteness;
        
        /**
         * 问题标识
         */
        private List<String> identifiedIssues;
        
        /**
         * 改进建议
         */
        private List<String> improvementSuggestions;
    }
}
