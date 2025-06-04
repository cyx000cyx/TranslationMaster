package com.translation.translate.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.translation.common.enums.ErrorCode;
import com.translation.common.exception.BusinessException;
import com.translation.translate.dto.DeepSeekResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek API客户端
 * 负责与DeepSeek翻译API的交互
 */
@Slf4j
@Component
public class DeepSeekClient {

    @Value("${deepseek.api.url:https://api.deepseek.com}")
    private String apiUrl;

    @Value("${deepseek.api.key:}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.timeout.seconds:30}")
    private Integer timeoutSeconds;

    // API路径
    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    /**
     * 翻译文本
     * 
     * @param text 原文本
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 翻译结果
     */
    public String translate(String text, String sourceLanguage, String targetLanguage) {
        log.debug("调用DeepSeek API翻译，源语言: {}, 目标语言: {}", sourceLanguage, targetLanguage);

        try {
            // 构建翻译提示词
            String prompt = buildTranslationPrompt(text, sourceLanguage, targetLanguage);

            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(prompt);

            // 发送请求
            String response = sendRequest(CHAT_COMPLETIONS_PATH, requestBody);

            // 解析响应
            String translatedText = parseTranslationResponse(response);

            log.debug("DeepSeek API翻译完成，原文长度: {}, 译文长度: {}", 
                    text.length(), translatedText.length());

            return translatedText;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用DeepSeek API失败", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, 
                    "DeepSeek API调用失败: " + e.getMessage());
        }
    }

    /**
     * 检查API健康状态
     * 
     * @return 是否健康
     */
    public boolean checkHealth() {
        try {
            // 发送简单的测试请求
            String testPrompt = "Hello, this is a health check.";
            Map<String, Object> requestBody = buildRequestBody(testPrompt);

            String response = sendRequest(CHAT_COMPLETIONS_PATH, requestBody);

            // 如果能正常获得响应，则认为API健康
            return StrUtil.isNotBlank(response);

        } catch (Exception e) {
            log.error("DeepSeek API健康检查失败", e);
            return false;
        }
    }

    /**
     * 构建翻译提示词
     */
    private String buildTranslationPrompt(String text, String sourceLanguage, String targetLanguage) {
        String sourceLangName = getLanguageName(sourceLanguage);
        String targetLangName = getLanguageName(targetLanguage);

        return String.format(
                "请将以下%s文本翻译成%s，保持原意和语气，只返回翻译结果，不要添加任何解释：\n\n%s",
                sourceLangName, targetLangName, text
        );
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 8000);
        requestBody.put("temperature", 0.3);
        requestBody.put("stream", false);

        // 构建消息
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        requestBody.put("messages", new Object[]{message});

        return requestBody;
    }

    /**
     * 发送HTTP请求
     */
    private String sendRequest(String path, Map<String, Object> requestBody) {
        // 检查API配置
        if (StrUtil.isBlank(apiKey)) {
            String envApiKey = System.getenv("DEEPSEEK_API_KEY");
            if (StrUtil.isNotBlank(envApiKey)) {
                apiKey = envApiKey;
            } else {
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, 
                        "DeepSeek API Key未配置");
            }
        }

        String fullUrl = apiUrl + path;

        log.debug("发送请求到DeepSeek API: {}", fullUrl);
        String responseBody;
        try (HttpResponse response = HttpRequest.post(fullUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(requestBody))
                .timeout((int) Duration.ofSeconds(timeoutSeconds).toMillis())
                .execute()) {

            if (!response.isOk()) {
                String errorMsg = String.format("DeepSeek API请求失败，状态码: %d, 响应: %s",
                        response.getStatus(), response.body());
                log.error(errorMsg);
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, errorMsg);
            }

            responseBody = response.body();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("发送HTTP请求失败", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
                    "网络请求失败: " + e.getMessage());
        }

        log.debug("DeepSeek API响应成功，响应长度: {}", responseBody.length());

        return responseBody;
    }

    /**
     * 解析翻译响应
     */
    private String parseTranslationResponse(String response) {
        try {
            DeepSeekResponse dsResponse = JSONUtil.toBean(response, DeepSeekResponse.class);

            // 检查错误和空值
            if (dsResponse.getError() != null) {
                String message = dsResponse.getError().getMessage();
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
                        "DeepSeek API返回错误: " + (message == null ? "未知错误" : message));
            }

            List<DeepSeekResponse.Choice> choices = dsResponse.getChoices();
            if (CollectionUtils.isEmpty(choices)) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
                        "DeepSeek API响应格式错误，choices为空!");
            }

            DeepSeekResponse.Choice firstChoice = choices.get(0);
            if (firstChoice.getMessage() == null) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
                        "DeepSeek API响应格式错误，message为空!");
            }

            // 提取翻译结果
            String content = firstChoice.getMessage().getContent();

            if (StrUtil.isBlank(content)) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, 
                        "DeepSeek API返回空的翻译结果");
            }

            return content.trim();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析DeepSeek API响应失败，响应内容: {}", response, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, 
                    "解析API响应失败: " + e.getMessage());
        }
    }

    /**
     * 获取语言名称
     */
    private String getLanguageName(String languageCode) {
        if (StrUtil.isBlank(languageCode)) {
            return "自动检测语言";
        }

        switch (languageCode.toLowerCase()) {
            case "zh":
            case "zh-cn":
                return "中文";
            case "zh-tw":
                return "繁体中文";
            case "en":
                return "英语";
            case "ja":
                return "日语";
            default:
                return languageCode;
        }
    }
}
