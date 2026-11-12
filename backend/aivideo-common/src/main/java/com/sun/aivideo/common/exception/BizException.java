package com.sun.aivideo.common.exception;

import com.sun.aivideo.common.enums.ErrorCode;
import lombok.Getter;

/** 业务异常的错误码、消息模板统一由 ErrorCode 定义。 */
@Getter
public class BizException extends RuntimeException {
    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode, Object... args) {
        super(errorCode.format(args));
        this.errorCode = errorCode;
    }

    public int getCode() {
        return errorCode.getCode();
    }
}
