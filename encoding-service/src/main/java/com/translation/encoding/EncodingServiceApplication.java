package com.translation.encoding;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 编码服务启动类
 * 负责多语言文本的高效编码和快速查询功能
 * 
 * @author translation-system
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"com.translation.encoding", "com.translation.common"})
@MapperScan("com.translation.encoding.mapper")
public class EncodingServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(EncodingServiceApplication.class, args);
        System.out.println("=================================");
        System.out.println("  编码服务启动成功!");
        System.out.println("  端口: 8004");
        System.out.println("  功能: 多语言文本编码与查询");
        System.out.println("=================================");
    }
}
