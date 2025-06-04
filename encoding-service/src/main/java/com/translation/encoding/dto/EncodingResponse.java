package com.translation.encoding.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 编码响应DTO
 */
@Data
public class EncodingResponse {
    private String encodingId;
    private String taskId;
    private long originalSize;
    private long compressedSize;
    private double compressionRatio;
    private LocalDateTime createTime;
    private String status;
}