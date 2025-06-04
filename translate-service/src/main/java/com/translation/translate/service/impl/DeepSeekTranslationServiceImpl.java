package com.translation.translate.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.translation.common.enums.LanguageEnum;
import com.translation.translate.service.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DeepSeek翻译服务实现
 * 可以替换为其他LLM翻译服务实现（GPT、Claude等）
 */
@Slf4j
@Service
public class DeepSeekTranslationServiceImpl implements TranslationService {
    
    @Value("${translation.deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String deepSeekApiUrl;
    
    @Value("${translation.deepseek.api.key:}")
    private String deepSeekApiKey;
    
    @Value("${translation.deepseek.model:deepseek-chat}")
    private String deepSeekModel;
    
    @Value("${translation.request.timeout:30000}")
    private int requestTimeout;

    private static final Map<String, String> languageNames = new ConcurrentHashMap<>();
    static {
        // 初始化语言名称映射
        languageNames.put(LanguageEnum.CHINESE_SIMPLIFIED.getCode(), LanguageEnum.CHINESE_SIMPLIFIED.getChineseName());
        languageNames.put(LanguageEnum.CHINESE_TRADITIONAL.getCode(), LanguageEnum.CHINESE_TRADITIONAL.getChineseName());
        languageNames.put(LanguageEnum.ENGLISH.getCode(), LanguageEnum.ENGLISH.getChineseName());
        languageNames.put(LanguageEnum.JAPANESE.getCode(), LanguageEnum.JAPANESE.getChineseName());
    }
    
    @Override
    public TranslationResult translateText(String sourceText, String sourceLanguage, String targetLanguage, Map<String, Object> options) {
        long startTime = System.currentTimeMillis();
        
        if (sourceText == null || sourceText.trim().isEmpty()) {
            return new TranslationResult(false, sourceText, null, sourceLanguage, targetLanguage, 
                "源文本为空", 0L, 0.0);
        }
        
        try {
            log.debug("开始翻译: {} -> {}, 文本长度: {}", sourceLanguage, targetLanguage, sourceText.length());
            
            // 构建翻译提示词
            String prompt = buildTranslationPrompt(sourceText, sourceLanguage, targetLanguage);
            
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.set("model", deepSeekModel);
            requestBody.set("messages", new Object[]{
                Map.of("role", "user", "content", prompt)
            });
            requestBody.set("temperature", 0.1);
            requestBody.set("max_tokens", Math.min(4000, sourceText.length() * 3));
            
            // 发送API请求
            HttpResponse response = HttpRequest.post(deepSeekApiUrl)
                .header("Authorization", "Bearer " + deepSeekApiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .timeout(requestTimeout)
                .execute();
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            if (!response.isOk()) {
                String error = "DeepSeek API请求失败: " + response.getStatus() + " " + response.body();
                log.error(error);
                return new TranslationResult(false, sourceText, null, sourceLanguage, targetLanguage, 
                    error, processingTime, 0.0);
            }
            
            // 解析响应
            JSONObject responseJson = JSONUtil.parseObj(response.body());
            if (!responseJson.containsKey("choices") || responseJson.getJSONArray("choices").isEmpty()) {
                return new TranslationResult(false, sourceText, null, sourceLanguage, targetLanguage, 
                    "API响应格式错误", processingTime, 0.0);
            }
            
            JSONObject choice = responseJson.getJSONArray("choices").getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String translatedText = message.getStr("content");
            
            if (translatedText == null || translatedText.trim().isEmpty()) {
                return new TranslationResult(false, sourceText, null, sourceLanguage, targetLanguage, 
                    "翻译结果为空", processingTime, 0.0);
            }
            
            // 清理翻译结果（移除可能的提示词回显）
            translatedText = cleanTranslationResult(translatedText);
            
            log.debug("翻译成功: {} -> {}, 处理时间: {}ms", sourceLanguage, targetLanguage, processingTime);
            
            return new TranslationResult(true, sourceText, translatedText, sourceLanguage, targetLanguage, 
                null, processingTime, 0.9);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("翻译异常: " + sourceLanguage + " -> " + targetLanguage, e);
            return new TranslationResult(false, sourceText, null, sourceLanguage, targetLanguage, 
                "翻译异常: " + e.getMessage(), processingTime, 0.0);
        }
    }
    
    @Override
    public BatchTranslationResult batchTranslateText(String sourceText, String sourceLanguage, String[] targetLanguages, Map<String, Object> options) {
        Map<String, TranslationResult> translations = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;
        
        log.info("开始批量翻译: {} -> {}, 目标语言数: {}", sourceLanguage, java.util.Arrays.toString(targetLanguages), targetLanguages.length);
        
        for (String targetLanguage : targetLanguages) {
            TranslationResult result = translateText(sourceText, sourceLanguage, targetLanguage, options);
            translations.put(targetLanguage, result);
            
            if (result.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }
            
            // 添加适当的延迟避免API限制
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        boolean overallSuccess = successCount > 0;
        log.info("批量翻译完成: 成功={}, 失败={}", successCount, failureCount);
        
        return new BatchTranslationResult(overallSuccess, sourceText, sourceLanguage, translations, 
            targetLanguages.length, successCount, failureCount, null);
    }
    
    @Override
    public MultiBatchTranslationResult multiBatchTranslateTexts(Map<String, String> sourceTexts, String sourceLanguage, String[] targetLanguages, Map<String, Object> options) {
        Map<String, BatchTranslationResult> results = new HashMap<>();
        int successFiles = 0;
        int failureFiles = 0;
        
        log.info("开始多文本批量翻译: 文件数={}, 目标语言数={}", sourceTexts.size(), targetLanguages.length);
        
        for (Map.Entry<String, String> entry : sourceTexts.entrySet()) {
            String fileName = entry.getKey();
            String sourceText = entry.getValue();
            
            log.debug("翻译文件: {}", fileName);
            
            BatchTranslationResult batchResult = batchTranslateText(sourceText, sourceLanguage, targetLanguages, options);
            results.put(fileName, batchResult);
            
            if (batchResult.isSuccess()) {
                successFiles++;
            } else {
                failureFiles++;
            }
        }
        
        boolean overallSuccess = successFiles > 0;
        log.info("多文本批量翻译完成: 成功文件={}, 失败文件={}", successFiles, failureFiles);
        
        return new MultiBatchTranslationResult(overallSuccess, sourceLanguage, targetLanguages, results, 
            sourceTexts.size(), successFiles, failureFiles, null);
    }
    
    @Override
    public boolean isServiceAvailable() {
        if (deepSeekApiKey == null || deepSeekApiKey.trim().isEmpty()) {
            log.warn("DeepSeek API密钥未配置");
            return false;
        }
        
        try {
            // 发送简单的测试请求
            JSONObject requestBody = new JSONObject();
            requestBody.set("model", deepSeekModel);
            requestBody.set("messages", new Object[]{
                Map.of("role", "user", "content", "Hello")
            });
            requestBody.set("max_tokens", 10);
            
            HttpResponse response = HttpRequest.post(deepSeekApiUrl)
                .header("Authorization", "Bearer " + deepSeekApiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .timeout(5000)
                .execute();
            
            return response.isOk();
            
        } catch (Exception e) {
            log.warn("DeepSeek服务可用性检查失败", e);
            return false;
        }
    }
    
    /**
     * 构建翻译提示词
     */
    private String buildTranslationPrompt(String sourceText, String sourceLanguage, String targetLanguage) {
        String sourceLangName = languageNames.getOrDefault(sourceLanguage, sourceLanguage);
        String targetLangName = languageNames.getOrDefault(targetLanguage, targetLanguage);
        
        return String.format(
            "请将以下%s文本翻译成%s。要求：\n" +
            "1. 保持原文的意思和语气\n" +
            "2. 使用自然流畅的表达\n" +
            "3. 直接输出翻译结果，不要添加解释\n\n" +
            "原文：\n%s",
            sourceLangName, targetLangName, sourceText
        );
    }
    
    /**
     * 清理翻译结果
     */
    private String cleanTranslationResult(String translatedText) {
        if (translatedText == null) return null;
        
        // 移除可能的提示词回显
        translatedText = translatedText.trim();
        
        // 移除常见的前缀
        String[] prefixesToRemove = {
            "翻译结果：", "翻译：", "译文：", "Translation:", "Result:"
        };
        
        for (String prefix : prefixesToRemove) {
            if (translatedText.startsWith(prefix)) {
                translatedText = translatedText.substring(prefix.length()).trim();
            }
        }
        
        return translatedText;
    }
}