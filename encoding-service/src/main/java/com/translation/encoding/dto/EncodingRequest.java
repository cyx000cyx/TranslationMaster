package com.translation.encoding.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 编码请求DTO
 */
@Data
public class EncodingRequest {

    @NotNull(message = "任务ID不能为空")
    private String taskId;

    @NotEmpty(message = "文本内容不能为空")
    private Map<String, String> texts;

    private String compressionType = "snappy";

    private boolean enableOptimization = true;
}