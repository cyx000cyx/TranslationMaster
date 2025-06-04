package com.translation.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 翻译任务实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("translation_task")
public class TranslationTask {
    
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 任务ID（UUID）
     */
    @TableField("task_id")
    private String taskId;
    
    /**
     * 任务类型
     */
    @TableField("task_type")
    private String taskType;
    
    /**
     * 音频文件目录路径
     */
    @TableField("audio_directory_path")
    private String audioDirectoryPath;
    
    /**
     * 源语言
     */
    @TableField("source_language")
    private String sourceLanguage;
    
    /**
     * 目标语言列表（逗号分隔）
     */
    @TableField("target_languages")
    private String targetLanguages;
    
    /**
     * 任务状态
     * @see com.translation.task.entity.TranslationTask.Status
     */
    @TableField("status")
    private String status;
    
    /**
     * 总文件数
     */
    @TableField("total_files")
    private Integer totalFiles;
    
    /**
     * 已处理文件数
     */
    @TableField("processed_files")
    private Integer processedFiles;
    
    /**
     * 成功处理文件数
     */
    @TableField("success_files")
    private Integer successFiles;
    
    /**
     * 失败文件数
     */
    @TableField("failed_files")
    private Integer failedFiles;
    
    /**
     * 进度百分比
     */
    @TableField("progress_percent")
    private Double progressPercent;
    
    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;
    
    /**
     * 结果文件路径
     */
    @TableField("result_file_path")
    private String resultFilePath;
    
    /**
     * 优先级
     */
    @TableField("priority")
    private Integer priority;
    
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;
    
    /**
     * 完成时间
     */
    @TableField("complete_time")
    private LocalDateTime completeTime;
    
    // 任务状态常量
    public static class Status {
        public static final String CREATED = "CREATED";
        public static final String PROCESSING = "PROCESSING";
        public static final String SPEECH_RECOGNITION = "SPEECH_RECOGNITION";
        public static final String TRANSLATION = "TRANSLATION";
        public static final String ENCODING = "ENCODING";
        public static final String COMPLETED = "COMPLETED";
        public static final String FAILED = "FAILED";
        public static final String CANCELLED = "CANCELLED";
    }
    
    // 任务类型常量
    public static class Type {
        public static final String AUDIO_TRANSLATION = "AUDIO_TRANSLATION";
        public static final String TEXT_TRANSLATION = "TEXT_TRANSLATION";
        public static final String BATCH_TRANSLATION = "BATCH_TRANSLATION";
    }
}