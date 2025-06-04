package com.translation.translate.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * LangChain4j配置类以及使用的HTTP客户端配置
 * 配置翻译服务相关的组件和缓存
 */
@Slf4j
@Configuration
@EnableCaching
public class LangChainConfig {

    @Value("${translation.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${translation.thread.pool.size:10}")
    private int threadPoolSize;

    @Value("${deepseek.api.timeout.seconds:30}")
    private int timeoutSeconds;

    /**
     * 配置翻译缓存管理器
     * 缓存翻译结果以提高性能
     */
    @Bean
    public CacheManager cacheManager() {
        log.info("配置翻译缓存管理器，缓存启用状态: {}", cacheEnabled);
        
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        if (cacheEnabled) {
            // 配置缓存名称
            cacheManager.setCacheNames(java.util.Arrays.asList(
                    "translations",      // 翻译结果缓存
                    "validations",       // 校验结果缓存
                    "language-detection", // 语言检测缓存
                    "health-checks"      // 健康检查缓存
            ));
            
            // 设置允许空值缓存
            cacheManager.setAllowNullValues(false);
        }
        
        return cacheManager;
    }

    /**
     * 配置翻译任务执行器
     * 用于并行处理批量翻译任务
     */
    @Bean("translationExecutor")
    public Executor translationExecutor() {
        log.info("配置翻译任务执行器，线程池大小: {}", threadPoolSize);
        
        return Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread thread = new Thread(r, "translation-worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 配置HTTP客户端设置
     * 用于DeepSeek API调用
     */
    @Bean
    public HttpClientConfig httpClientConfig() {
        HttpClientConfig config = new HttpClientConfig();
        config.setConnectTimeout(timeoutSeconds * 1000);
        config.setReadTimeout(timeoutSeconds * 1000);
        config.setMaxRetries(3);
        config.setRetryDelay(1000);
        
        log.info("配置HTTP客户端，超时时间: {}秒", timeoutSeconds);
        
        return config;
    }

    /**
     * 翻译质量评估器配置
     */
    @Bean
    public QualityAssessorConfig qualityAssessorConfig() {
        QualityAssessorConfig config = new QualityAssessorConfig();
        
        // 配置质量评估阈值
        config.setExcellentThreshold(0.95);
        config.setGoodThreshold(0.85);
        config.setFairThreshold(0.7);
        config.setPoorThreshold(0.5);
        
        // 配置评估权重
        config.setAccuracyWeight(0.4);
        config.setFluencyWeight(0.3);
        config.setCompletenessWeight(0.3);
        
        return config;
    }

    /**
     * 语言检测配置
     */
    @Bean
    public LanguageDetectorConfig languageDetectorConfig() {
        LanguageDetectorConfig config = new LanguageDetectorConfig();
        
        // 配置检测阈值
        config.setConfidenceThreshold(0.7);
        config.setMinTextLength(10);
        config.setMaxTextLength(1000);
        
        // 配置支持的语言
        config.setSupportedLanguages(java.util.Arrays.asList(
                "zh", "en", "ja"
        ));
        
        return config;
    }

    /**
     * HTTP客户端配置类
     */
    @Data
    public static class HttpClientConfig {
        private int connectTimeout;
        private int readTimeout;
        private int maxRetries;
        private int retryDelay;
    }

    /**
     * 质量评估器配置类
     */
    @Data
    public static class QualityAssessorConfig {
        private double excellentThreshold;
        private double goodThreshold;
        private double fairThreshold;
        private double poorThreshold;
        private double accuracyWeight;
        private double fluencyWeight;
        private double completenessWeight;
    }

    /**
     * 语言检测器配置类
     */
    @Data
    public static class LanguageDetectorConfig {
        private double confidenceThreshold;
        private int minTextLength;
        private int maxTextLength;
        private java.util.List<String> supportedLanguages;
    }
}
