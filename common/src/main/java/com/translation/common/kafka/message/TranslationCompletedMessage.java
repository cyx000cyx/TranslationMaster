package com.translation.common.kafka.message;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 翻译完成消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslationCompletedMessage {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 音频文件目录路径
     */
    private String audioDirectoryPath;
    
    /**
     * 翻译结果列表
     */
    private List<TranslationResult> translationResults;
    
    /**
     * 处理完成时间
     */
    private LocalDateTime completedTime;
    
    /**
     * 翻译结果详情
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranslationResult {
        /**
         * 音频文件名
         */
        private String audioFileName;
        
        /**
         * 原始文本
         */
        private String originalText;
        
        /**
         * 翻译结果（语言代码 -> 翻译文本）
         */
        private Map<String, String> translations;
        
        /**
         * 翻译结果文件路径
         */
        private String translationFilePath;
    }
}