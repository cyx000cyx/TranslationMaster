package com.translation.common.kafka.consumer;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * 内存感知的Kafka消费者基类
 * 当内存使用率超过50%时停止消费
 */
@Slf4j
public abstract class MemoryAwareConsumer {
    
    private static final double MEMORY_THRESHOLD = 0.5; // 50%内存阈值
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    /**
     * 检查内存使用率是否超过阈值
     */
    protected boolean shouldStopConsuming() {
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        
        if (maxMemory == -1) {
            maxMemory = memoryBean.getHeapMemoryUsage().getCommitted();
        }
        
        double memoryUsageRatio = (double) usedMemory / maxMemory;
        
        if (memoryUsageRatio > MEMORY_THRESHOLD) {
            log.warn("内存使用率超过{}%，暂停消息消费", MEMORY_THRESHOLD * 100);
            return true;
        }
        
        return false;
    }
    
    /**
     * 强制垃圾回收
     */
    protected void forceGarbageCollection() {
        log.info("执行垃圾回收以释放内存");
        System.gc();
        System.runFinalization();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}