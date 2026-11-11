package com.sun.aivideo.ai.exception;

import lombok.Getter;

/**
 * AI 模块统一异常：调用厂商接口失败、参数不支持等。
 */
@Getter
public class AiClientException extends RuntimeException {

    /** 厂商返回的错误码，或模块内部错误码 */
    private final String errorCode;

    public AiClientException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AiClientException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
