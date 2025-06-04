package com.translation.translate.dto;

import com.translation.common.enums.LanguageEnum;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 翻译请求DTO
 * 用于接收翻译服务的请求参数
 */
@Data
public class TranslateRequest {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 源文本（单个文本翻译时使用）
     */
    @Size(max = 10000, message = "源文本长度不能超过10000个字符")
    private String sourceText;

    /**
     * 源文本列表（批量翻译时使用）
     */
    private List<@NotBlank @Size(max = 5000, message = "批量翻译中单个文本长度不能超过5000个字符") String> texts;

    /**
     * 源语言代码
     * @see LanguageEnum
     */
    private String sourceLanguage;

    /**
     * 目标语言列表
     * @see LanguageEnum
     */
    @NotEmpty(message = "目标语言不能为空")
    private List<String> targetLanguages;

    /**
     * 翻译质量级别
     * FAST - 快速翻译
     * BALANCED - 平衡模式（默认）
     * ACCURATE - 高精度翻译
     */
    private String qualityLevel = "BALANCED";

    /**
     * 是否保留格式
     */
    private Boolean preserveFormatting = true;

    /**
     * 翻译上下文
     * 可以提供领域信息帮助翻译
     */
    @Size(max = 500, message = "翻译上下文长度不能超过500个字符")
    private String context;

    /**
     * 专业术语词典
     * 用于确保特定术语的翻译一致性
     */
    private List<TermEntry> terminology;

    /**
     * 翻译风格
     * FORMAL - 正式
     * INFORMAL - 非正式
     * TECHNICAL - 技术文档
     * LITERARY - 文学
     */
    private String style = "FORMAL";

    /**
     * 请求开始时间（用于性能统计）
     */
    private Long startTime = System.currentTimeMillis();

    /**
     * 术语条目
     */
    @Data
    public static class TermEntry {
        /**
         * 源术语
         */
        private String source;

        /**
         * 目标术语（按语言代码映射）
         */
        private java.util.Map<String, String> targets;
    }
}
