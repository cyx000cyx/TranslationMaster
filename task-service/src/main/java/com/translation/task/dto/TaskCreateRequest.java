package com.translation.task.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 任务创建请求DTO
 * 用于接收前端创建任务的请求参数
 */
@Data
public class TaskCreateRequest {

    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称长度不能超过100个字符")
    private String taskName;

    /**
     * 任务类型：TEXT-文本翻译，AUDIO-音频翻译
     */
    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    /**
     * 源语言代码
     */
    private String sourceLanguage;

    /**
     * 目标语言列表
     */
    @NotEmpty(message = "目标语言不能为空")
    private List<String> targetLanguages;

    /**
     * 原始文本内容（文本翻译时必填）
     */
    private String originalText;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 备注信息
     */
    private String remarks;

    /**
     * 是否需要文本校验
     * 用于音频翻译时验证STT识别结果的准确性
     */
    private Boolean needValidation = true;

    /**
     * 优先级（1-低，2-中，3-高）
     */
    private Integer priority = 2;
}
