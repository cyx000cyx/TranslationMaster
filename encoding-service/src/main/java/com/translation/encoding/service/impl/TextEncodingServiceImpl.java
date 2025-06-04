package com.translation.encoding.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.translation.encoding.dto.EncodingRequest;
import com.translation.encoding.dto.EncodingResponse;
import com.translation.encoding.dto.QueryRequest;
import com.translation.encoding.service.TextEncodingService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xerial.snappy.Snappy;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文本编码服务实现类
 * 提供高效的多语言文本编码、压缩和快速查询功能
 */
@Slf4j
@Service
public class TextEncodingServiceImpl implements TextEncodingService {

    // 内存存储，生产环境可替换为数据库或分布式缓存
    private final Map<String, EncodedData> encodingStorage = new ConcurrentHashMap<>();
    private final Map<String, CompressionStats> statsStorage = new ConcurrentHashMap<>();

    @Override
    public EncodingResponse encodeTexts(EncodingRequest request) {
        log.info("开始编码文本，任务ID: {}, 语言数量: {}", request.getTaskId(), request.getTexts().size());
        
        String encodingId = IdUtil.simpleUUID();
        
        try {
            // 计算原始大小
            long originalSize = calculateOriginalSize(request.getTexts());
            
            // 编码文本数据
            EncodedData encodedData = encodeTextData(request);
            
            // 压缩编码后的数据
            byte[] compressedData = compressData(encodedData.getData());
            encodedData.setCompressedData(compressedData);
            
            // 存储编码数据
            encodingStorage.put(encodingId, encodedData);
            
            // 计算压缩统计
            double compressionRatio = (double) compressedData.length / originalSize;
            CompressionStats stats = new CompressionStats(
                    originalSize, compressedData.length, compressionRatio, LocalDateTime.now()
            );
            statsStorage.put(encodingId, stats);
            
            // 构建响应
            EncodingResponse response = new EncodingResponse();
            response.setEncodingId(encodingId);
            response.setTaskId(request.getTaskId());
            response.setOriginalSize(originalSize);
            response.setCompressedSize(compressedData.length);
            response.setCompressionRatio(compressionRatio);
            response.setCreateTime(LocalDateTime.now());
            response.setStatus("SUCCESS");
            
            log.info("文本编码完成，编码ID: {}, 压缩率: {}%",
                    encodingId, (1 - compressionRatio) * 100);
            
            return response;
            
        } catch (Exception e) {
            log.error("文本编码失败，任务ID: " + request.getTaskId(), e);
            throw new RuntimeException("编码失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Object queryText(QueryRequest request) {
        log.info("查询文本，编码ID: {}, 语言: {}, 索引: {}", 
                request.getEncodingId(), request.getLanguage(), request.getTextIndex());
        
        EncodedData encodedData = encodingStorage.get(request.getEncodingId());
        if (encodedData == null) {
            throw new RuntimeException("编码数据不存在: " + request.getEncodingId());
        }
        
        try {
            // 解压数据
            byte[] decompressedData = decompressData(encodedData.getCompressedData());
            Map<String, Object> textData = JSONUtil.toBean(new String(decompressedData), Map.class);
            
            if (request.getLanguage() != null) {
                // 查询特定语言的文本
                Map<String, Object> languageData = (Map<String, Object>) textData.get(request.getLanguage());
                if (languageData == null) {
                    return Collections.emptyMap();
                }
                
                if (request.getTextIndex() != null) {
                    // 查询特定索引的文本
                    return languageData.get(request.getTextIndex().toString());
                } else {
                    // 返回该语言的所有文本
                    return languageData;
                }
            } else {
                // 返回所有语言的文本
                return textData;
            }
            
        } catch (Exception e) {
            log.error("查询文本失败，编码ID: " + request.getEncodingId(), e);
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Object batchQueryTexts(List<QueryRequest> requests) {
        log.info("批量查询文本，请求数量: {}", requests.size());
        
        List<Object> results = new ArrayList<>();
        for (QueryRequest request : requests) {
            try {
                Object result = queryText(request);
                results.add(Map.of(
                        "request", request,
                        "success", true,
                        "data", result
                ));
            } catch (Exception e) {
                results.add(Map.of(
                        "request", request,
                        "success", false,
                        "error", e.getMessage()
                ));
            }
        }
        
        return Map.of(
                "total", requests.size(),
                "results", results
        );
    }

    @Override
    public Object getEncodingInfo(String encodingId) {
        log.info("获取编码信息，编码ID: {}", encodingId);
        
        EncodedData encodedData = encodingStorage.get(encodingId);
        CompressionStats stats = statsStorage.get(encodingId);
        
        if (encodedData == null) {
            throw new RuntimeException("编码数据不存在: " + encodingId);
        }
        
        return Map.of(
                "encodingId", encodingId,
                "taskId", encodedData.getTaskId(),
                "languageCount", encodedData.getLanguageCount(),
                "textCount", encodedData.getTextCount(),
                "createTime", encodedData.getCreateTime(),
                "compressionStats", stats != null ? stats : "统计信息不可用"
        );
    }

    @Override
    public Object decodeTexts(String encodingId) {
        log.info("解码文本包，编码ID: {}", encodingId);
        
        EncodedData encodedData = encodingStorage.get(encodingId);
        if (encodedData == null) {
            throw new RuntimeException("编码数据不存在: " + encodingId);
        }
        
        try {
            byte[] decompressedData = decompressData(encodedData.getCompressedData());
            Map<String, Object> textData = JSONUtil.toBean(new String(decompressedData), Map.class);
            
            return Map.of(
                    "encodingId", encodingId,
                    "taskId", encodedData.getTaskId(),
                    "decodedTexts", textData,
                    "decodeTime", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            log.error("解码文本包失败，编码ID: " + encodingId, e);
            throw new RuntimeException("解码失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteEncoding(String encodingId) {
        log.info("删除编码数据，编码ID: {}", encodingId);
        
        encodingStorage.remove(encodingId);
        statsStorage.remove(encodingId);
        
        log.info("编码数据删除完成，编码ID: {}", encodingId);
    }

    @Override
    public Map<String, Object> getEncodingStatistics() {
        log.info("获取编码统计信息");
        
        int totalEncodings = encodingStorage.size();
        long totalOriginalSize = statsStorage.values().stream()
                .mapToLong(CompressionStats::getOriginalSize)
                .sum();
        long totalCompressedSize = statsStorage.values().stream()
                .mapToLong(CompressionStats::getCompressedSize)
                .sum();
        
        double avgCompressionRatio = totalOriginalSize > 0 ? 
                (double) totalCompressedSize / totalOriginalSize : 0.0;
        
        return Map.of(
                "totalEncodings", totalEncodings,
                "totalOriginalSize", totalOriginalSize,
                "totalCompressedSize", totalCompressedSize,
                "avgCompressionRatio", avgCompressionRatio,
                "spaceSaved", totalOriginalSize - totalCompressedSize,
                "spaceSavedPercentage", (1 - avgCompressionRatio) * 100
        );
    }

    @Override
    public Object analyzeCompression(String encodingId) {
        log.info("分析压缩率，编码ID: {}", encodingId);
        
        CompressionStats stats = statsStorage.get(encodingId);
        if (stats == null) {
            throw new RuntimeException("压缩统计信息不存在: " + encodingId);
        }
        
        String compressionLevel;
        if (stats.getCompressionRatio() < 0.3) {
            compressionLevel = "优秀";
        } else if (stats.getCompressionRatio() < 0.6) {
            compressionLevel = "良好";
        } else if (stats.getCompressionRatio() < 0.8) {
            compressionLevel = "一般";
        } else {
            compressionLevel = "较差";
        }
        
        return Map.of(
                "encodingId", encodingId,
                "originalSize", stats.getOriginalSize(),
                "compressedSize", stats.getCompressedSize(),
                "compressionRatio", stats.getCompressionRatio(),
                "spaceSaved", stats.getOriginalSize() - stats.getCompressedSize(),
                "spaceSavedPercentage", (1 - stats.getCompressionRatio()) * 100,
                "compressionLevel", compressionLevel,
                "createTime", stats.getCreateTime()
        );
    }

    @Override
    public Object optimizeEncoding(String encodingId) {
        log.info("优化编码，编码ID: {}", encodingId);
        
        EncodedData encodedData = encodingStorage.get(encodingId);
        if (encodedData == null) {
            throw new RuntimeException("编码数据不存在: " + encodingId);
        }
        
        try {
            // 重新编码和压缩
            byte[] originalData = decompressData(encodedData.getCompressedData());
            byte[] optimizedData = compressData(originalData);
            
            long originalCompressedSize = encodedData.getCompressedData().length;
            long optimizedSize = optimizedData.length;
            
            if (optimizedSize < originalCompressedSize) {
                encodedData.setCompressedData(optimizedData);
                encodingStorage.put(encodingId, encodedData);
                
                // 更新统计信息
                CompressionStats stats = statsStorage.get(encodingId);
                if (stats != null) {
                    stats.setCompressedSize(optimizedSize);
                    stats.setCompressionRatio((double) optimizedSize / stats.getOriginalSize());
                    statsStorage.put(encodingId, stats);
                }
                
                log.info("编码优化完成，编码ID: {}, 优化前: {} bytes, 优化后: {} bytes", 
                        encodingId, originalCompressedSize, optimizedSize);
            }
            
            return Map.of(
                    "encodingId", encodingId,
                    "optimized", optimizedSize < originalCompressedSize,
                    "originalSize", originalCompressedSize,
                    "optimizedSize", optimizedSize,
                    "spaceSaved", originalCompressedSize - optimizedSize,
                    "optimizationTime", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            log.error("编码优化失败，编码ID: " + encodingId, e);
            throw new RuntimeException("优化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 编码文本数据
     */
    private EncodedData encodeTextData(EncodingRequest request) {
        Map<String, Object> encodedTexts = new HashMap<>();
        int textCount = 0;
        
        for (Map.Entry<String, String> entry : request.getTexts().entrySet()) {
            String language = entry.getKey();
            String text = entry.getValue();
            
            // 为每个语言的文本分配索引
            Map<String, String> languageTexts = new HashMap<>();
            languageTexts.put("0", text); // 简单索引，可扩展为多文本支持
            
            encodedTexts.put(language, languageTexts);
            textCount++;
        }
        
        String jsonData = JSONUtil.toJsonStr(encodedTexts);
        
        EncodedData encodedData = new EncodedData();
        encodedData.setTaskId(request.getTaskId());
        encodedData.setData(jsonData.getBytes());
        encodedData.setLanguageCount(request.getTexts().size());
        encodedData.setTextCount(textCount);
        encodedData.setCreateTime(LocalDateTime.now());
        
        return encodedData;
    }

    /**
     * 计算原始数据大小
     */
    private long calculateOriginalSize(Map<String, String> texts) {
        return texts.values().stream()
                .mapToLong(text -> text.getBytes().length)
                .sum();
    }

    /**
     * 压缩数据
     */
    private byte[] compressData(byte[] data) throws Exception {
        return Snappy.compress(data);
    }

    /**
     * 解压数据
     */
    private byte[] decompressData(byte[] compressedData) throws Exception {
        return Snappy.uncompress(compressedData);
    }

    /**
     * 编码数据实体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class EncodedData {
        private String taskId;
        private byte[] data;
        private byte[] compressedData;
        private int languageCount;
        private int textCount;
        private LocalDateTime createTime;
    }

    /**
     * 压缩统计实体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class CompressionStats {
        private long originalSize;
        private long compressedSize;
        private double compressionRatio;
        private LocalDateTime createTime;
    }
}