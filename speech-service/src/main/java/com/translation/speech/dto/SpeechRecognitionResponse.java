package com.translation.speech.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 语音识别响应DTO
 * 返回语音识别的结果信息
 */
@Data
public class SpeechRecognitionResponse {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 识别的文本内容
     */
    private String recognizedText;

    /**
     * 检测到的语言
     */
    private String language;

    /**
     * 整体置信度（0.0-1.0）
     */
    private Double confidence;

    /**
     * 音频总时长（秒）
     */
    private Double duration;

    /**
     * 处理耗时（毫秒）
     */
    private Long processingTime;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 分段信息列表
     */
    private List<Object> segments;

    /**
     * 音频特征信息
     */
    private AudioFeatures audioFeatures;

    /**
     * 质量评估
     */
    private QualityAssessment qualityAssessment;

    /**
     * 处理时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 音频特征信息
     */
    @Data
    public static class AudioFeatures {
        /**
         * 采样率
         */
        private Integer sampleRate;
        
        /**
         * 声道数
         */
        private Integer channels;
        
        /**
         * 比特率
         */
        private Integer bitrate;
        
        /**
         * 文件格式
         */
        private String format;
        
        /**
         * 音量级别
         */
        private Double volumeLevel;
        
        /**
         * 信噪比
         */
        private Double signalToNoiseRatio;
    }

    /**
     * 质量评估信息
     */
    @Data
    public static class QualityAssessment {
        /**
         * 音频质量评分（0-100）
         */
        private Integer audioQualityScore;
        
        /**
         * 识别质量评分（0-100）
         */
        private Integer recognitionQualityScore;
        
        /**
         * 是否有背景噪音
         */
        private Boolean hasBackgroundNoise;
        
        /**
         * 语速评估
         */
        private String speechRate;
        
        /**
         * 音量是否合适
         */
        private Boolean volumeAppropriate;
        
        /**
         * 建议改进点
         */
        private List<String> improvementSuggestions;
    }
}
