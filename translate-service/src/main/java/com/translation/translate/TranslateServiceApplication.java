package com.translation.translate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 翻译服务启动类
 * 负责多语言翻译功能，集成DeepSeek API和LangChain4j
 * 
 * @author translation-system
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"com.translation.translate", "com.translation.common"})
public class TranslateServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TranslateServiceApplication.class, args);
        System.out.println("=================================");
        System.out.println("  翻译服务启动成功!");
        System.out.println("  端口: 8003");
        System.out.println("  功能: 多语言翻译与文本校验");
        System.out.println("=================================");
    }
}
