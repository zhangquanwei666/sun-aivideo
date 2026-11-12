package com.sun.aivideo.handler;

import com.sun.aivideo.ai.exception.AiClientException;
import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.common.enums.ErrorCode;
import com.sun.aivideo.common.exception.BizException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(cn.dev33.satoken.exception.NotLoginException.class)
    public BaseResponse handleLogin(cn.dev33.satoken.exception.NotLoginException e) {
        return BaseResponse.fail(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(BizException.class)
    public BaseResponse handleBiz(BizException e) {
        // message 可能包含 ErrorCode 模板格式化后的业务参数，因此不能重新取枚举的原始模板。
        return BaseResponse.fail(e.getCode(), e.getMessage());
    }

    /** AI 模块调用厂商失败 */
    @ExceptionHandler(AiClientException.class)
    public BaseResponse handleAi(AiClientException e) {
        return BaseResponse.fail(ErrorCode.PROVIDER_ERROR.getCode(), e.getMessage());
    }

    /** 覆盖 @RequestBody 校验（MethodArgumentNotValidException 是其子类）与 GET 参数绑定校验 */
    @ExceptionHandler(BindException.class)
    public BaseResponse handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                // 类型转换失败（如 id=abc）时不暴露框架内部信息
                .map(err -> err.isBindingFailure() ? ErrorCode.INVALID_PARAMETER.format(err.getField()) : err.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("；"));
        return BaseResponse.fail(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse handleConstraint(ConstraintViolationException e) {
        return BaseResponse.fail(ErrorCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse handleUnreadable(HttpMessageNotReadableException e) {
        return BaseResponse.fail(ErrorCode.MALFORMED_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public BaseResponse handleUploadSize(MaxUploadSizeExceededException e) {
        return BaseResponse.fail(ErrorCode.UPLOAD_TOO_LARGE);
    }

    @ExceptionHandler(DataAccessException.class)
    public BaseResponse handleDataAccess(DataAccessException e) {
        log.error("数据库访问失败", e);
        return BaseResponse.fail(ErrorCode.DATABASE_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse handleOther(Exception e) {
        log.error("未处理的异常", e);
        return BaseResponse.fail(ErrorCode.INTERNAL_ERROR);
    }
}
