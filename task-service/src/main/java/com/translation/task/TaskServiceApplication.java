package com.translation.task;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 任务服务启动类
 * 负责翻译任务的创建、查询、取消等管理功能
 */
@SpringBootApplication(scanBasePackages = {"com.translation.task", "com.translation.common"})
@MapperScan("com.translation.task.mapper")
public class TaskServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
        System.out.println("=================================");
        System.out.println("  任务服务启动成功!");
        System.out.println("  端口: 8001");
        System.out.println("  功能: 翻译任务管理");
        System.out.println("=================================");
    }
}
