package com.translation.translate.dto;

import lombok.Data;

import java.util.List;

@Data
public class DeepSeekResponse {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    private String systemFingerprint;
    private ApiError error;

    @Data
    public static class Choice {
        private int index;
        private Message message;
        private Object logprobs;
        private String finishReason;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    public static class Usage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
        private PromptTokensDetails promptTokensDetails;
        private int promptCacheHitTokens;
        private int promptCacheMissTokens;
    }

    @Data
    public static class PromptTokensDetails {
        private int cachedTokens;
    }

    @Data
    public static class ApiError {
        private String message;
        private String type;
        private String param;
        private String code;
    }
}
