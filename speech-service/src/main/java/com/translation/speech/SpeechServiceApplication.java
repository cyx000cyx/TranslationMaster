package com.translation.speech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 语音服务启动类
 * 负责音频文件的语音识别功能，集成Whisper
 * 
 * @author translation-system
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"com.translation.speech", "com.translation.common"})
public class SpeechServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SpeechServiceApplication.class, args);
        System.out.println("=================================");
        System.out.println("  语音服务启动成功!");
        System.out.println("  端口: 8002");
        System.out.println("  功能: Whisper语音识别");
        System.out.println("=================================");
    }
}
