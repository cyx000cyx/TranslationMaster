package com.translation.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 支持的语言枚举
 * 定义系统支持的所有翻译语言
 */
@Getter
public enum LanguageEnum {

    // 中文
    CHINESE_SIMPLIFIED("zh-CN", "zh", "简体中文", "Chinese (Simplified)"),
    CHINESE_TRADITIONAL("zh-TW", "zh-tw", "繁体中文", "Chinese (Traditional)"),

    // 英语
    ENGLISH("en", "en", "英语", "English"),

    // 日语
    JAPANESE("ja", "ja", "日语", "Japanese"),

    // 自动检测
    AUTO_DETECT("auto", "auto", "自动检测", "Auto Detect");

    private final String code;
    private final String iso639;
    private final String chineseName;
    private final String englishName;

    LanguageEnum(String code, String iso639, String chineseName, String englishName) {
        this.code = code;
        this.iso639 = iso639;
        this.chineseName = chineseName;
        this.englishName = englishName;
    }

    /**
     * 根据语言代码获取语言枚举
     */
    public static LanguageEnum fromCode(String code) {
        if (code == null) {
            return AUTO_DETECT;
        }

        return Arrays.stream(values())
                .filter(lang -> lang.getCode().equalsIgnoreCase(code) || 
                               lang.getIso639().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }


    @Override
    public String toString() {
        return String.format("%s (%s)", chineseName, code);
    }
}
