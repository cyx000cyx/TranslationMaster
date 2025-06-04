package com.translation.common.enums;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {

    INVALID_PARAM(400, "无效参数"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统错误"),

    // 外部API相关错误
    EXTERNAL_API_ERROR(5001, "外部API调用失败");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}