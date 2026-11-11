package com.sun.aivideo.common.exception;

import lombok.Getter;

/**
 * 业务异常，message 会直接返回给前端。
 */
@Getter
public class BizException extends RuntimeException {

    public static final int BAD_REQUEST = 400;
    public static final int NOT_FOUND = 404;
    public static final int PROVIDER_ERROR = 502;

    private final int code;

    public BizException(String message) {
        this(BAD_REQUEST, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
