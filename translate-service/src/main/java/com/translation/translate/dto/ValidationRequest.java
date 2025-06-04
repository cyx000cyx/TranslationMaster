package com.translation.translate.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 文本校验请求DTO
 * 用于STT识别结果与原始文本的准确性验证
 */
@Data
public class ValidationRequest {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 原始文本
     */
    @NotBlank(message = "原始文本不能为空")
    @Size(max = 10000, message = "原始文本长度不能超过10000个字符")
    private String originalText;

    /**
     * STT识别文本
     */
    @NotBlank(message = "识别文本不能为空")
    @Size(max = 10000, message = "识别文本长度不能超过10000个字符")
    private String recognizedText;

    /**
     * 相似度阈值（0.0-1.0）
     * 超过此阈值认为验证通过
     */
    @DecimalMin(value = "0.0", message = "相似度阈值不能小于0.0")
    @DecimalMax(value = "1.0", message = "相似度阈值不能大于1.0")
    private Double threshold = 0.8;

    /**
     * 验证模式
     * STRICT - 严格模式，要求高度相似
     * NORMAL - 普通模式（默认）
     * LENIENT - 宽松模式，允许较大差异
     */
    private String validationMode = "NORMAL";

    /**
     * 语言代码
     * 用于选择合适的验证算法
     */
    private String languageCode;

    /**
     * 是否忽略标点符号
     */
    private Boolean ignorePunctuation = true;

    /**
     * 是否忽略大小写
     */
    private Boolean ignoreCase = true;

    /**
     * 是否忽略空白字符
     */
    private Boolean ignoreWhitespace = true;

    /**
     * 权重配置
     * 用于调整不同相似度算法的权重
     */
    private WeightConfig weightConfig;

    /**
     * 关键词列表
     * 用于重点验证某些关键词的识别准确性
     */
    private java.util.List<String> keywords;

    /**
     * 领域类型
     * 不同领域可能需要不同的验证策略
     */
    private String domain;

    /**
     * 音频质量信息
     * 用于评估识别准确性的参考
     */
    private AudioQualityInfo audioQuality;

    /**
     * 权重配置
     */
    @Data
    public static class WeightConfig {
        /**
         * 编辑距离权重
         */
        private Double levenshteinWeight = 0.4;
        
        /**
         * Jaccard相似度权重
         */
        private Double jaccardWeight = 0.3;
        
        /**
         * 余弦相似度权重
         */
        private Double cosineWeight = 0.3;
        
        /**
         * 关键词匹配权重
         */
        private Double keywordWeight = 0.0;
    }

    /**
     * 音频质量信息
     */
    @Data
    public static class AudioQualityInfo {
        /**
         * 信噪比
         */
        private Double signalToNoiseRatio;
        
        /**
         * 音量级别
         */
        private Double volumeLevel;
        
        /**
         * 采样率
         */
        private Integer sampleRate;
        
        /**
         * 音频时长（秒）
         */
        private Double duration;
        
        /**
         * 是否有背景噪音
         */
        private Boolean hasBackgroundNoise;
    }
}
