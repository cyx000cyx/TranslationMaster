package com.translation.encoding.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 查询请求DTO
 */
@Data
public class QueryRequest {

    @NotBlank(message = "编码ID不能为空")
    private String encodingId;

    private String language;
    private Integer textIndex;
    private String queryType = "text";

    @Override
    public String toString() {
        return "QueryRequest{" +
                "encodingId='" + encodingId + '\'' +
                ", language='" + language + '\'' +
                ", textIndex=" + textIndex +
                ", queryType='" + queryType + '\'' +
                '}';
    }
}