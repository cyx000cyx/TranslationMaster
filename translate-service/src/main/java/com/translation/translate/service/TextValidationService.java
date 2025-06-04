package com.translation.translate.service;

import com.translation.translate.dto.ValidationRequest;
import com.translation.translate.dto.ValidationResponse;

/**
 * 文本校验服务接口
 * 用于验证STT识别结果与原始文本的准确性
 */
public interface TextValidationService {

    /**
     * 验证文本相似性
     * 对比STT识别结果与原始文本的准确性
     * 
     * @param request 校验请求
     * @return 校验结果
     */
    ValidationResponse validateText(ValidationRequest request);

    /**
     * 计算文本相似度
     * 
     * @param text1 文本1
     * @param text2 文本2
     * @return 相似度分数 (0.0-1.0)
     */
    double calculateSimilarity(String text1, String text2);

    /**
     * 检测文本语言
     * 
     * @param text 待检测文本
     * @return 语言代码
     */
    String detectLanguage(String text);

    /**
     * 纠错建议
     * 
     * @param originalText 原始文本
     * @param recognizedText 识别文本
     * @return 纠错建议
     */
    Object getCorrectSuggestions(String originalText, String recognizedText);
}
