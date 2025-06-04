package com.translation.speech.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 语音识别请求DTO
 * 用于接收语音识别的请求参数
 */
@Data
public class SpeechRecognitionRequest {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 音频文件路径（用于按路径识别）
     */
    private String filePath;

    /**
     * 指定语言代码（如：zh、en、ja等）
     * 如果不指定则自动检测
     */
    private String language;

    /**
     * 识别模式
     * transcribe - 转录（默认）
     * translate - 翻译为英语
     */
    private String mode = "transcribe";

    /**
     * 输出格式
     * json - JSON格式（默认）
     * text - 纯文本格式
     * srt - 字幕格式
     * vtt - WebVTT格式
     */
    private String outputFormat = "json";

    /**
     * 是否包含时间戳
     */
    private Boolean includeTimestamps = true;

    /**
     * 是否包含分段信息
     */
    private Boolean includeSegments = true;

    /**
     * 温度参数（0.0-1.0）
     * 控制输出的随机性，0表示最确定的输出
     */
    private Double temperature = 0.0;

    /**
     * 初始提示文本
     * 可以提供上下文信息帮助识别
     */
    @Size(max = 500, message = "初始提示文本长度不能超过500个字符")
    private String initialPrompt;

    /**
     * 是否启用语音活动检测
     */
    private Boolean enableVad = true;

    /**
     * 最大分段长度（秒）
     */
    private Integer maxSegmentLength = 30;

    /**
     * 音频预处理选项
     */
    private Boolean enablePreprocessing = false;

    /**
     * 降噪级别（0-3）
     */
    private Integer noiseReductionLevel = 1;
}
