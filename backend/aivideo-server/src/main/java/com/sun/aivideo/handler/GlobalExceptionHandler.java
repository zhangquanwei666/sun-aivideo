package com.sun.aivideo.handler;

import com.sun.aivideo.ai.exception.AiClientException;
import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.common.exception.BizException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
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

    @ExceptionHandler(BizException.class)
    public BaseResponse handleBiz(BizException e) {
        return BaseResponse.fail(e.getCode(), e.getMessage());
    }

    /** AI 模块调用厂商失败 */
    @ExceptionHandler(AiClientException.class)
    public BaseResponse handleAi(AiClientException e) {
        return BaseResponse.fail(BizException.PROVIDER_ERROR, e.getMessage());
    }

    /** 覆盖 @RequestBody 校验（MethodArgumentNotValidException 是其子类）与 GET 参数绑定校验 */
    @ExceptionHandler(BindException.class)
    public BaseResponse handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                // 类型转换失败（如 id=abc）时不暴露框架内部信息
                .map(err -> err.isBindingFailure() ? "参数 " + err.getField() + " 格式不正确" : err.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("；"));
        return BaseResponse.fail(BizException.BAD_REQUEST, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse handleConstraint(ConstraintViolationException e) {
        return BaseResponse.fail(BizException.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse handleUnreadable(HttpMessageNotReadableException e) {
        return BaseResponse.fail(BizException.BAD_REQUEST, "请求体格式错误");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public BaseResponse handleUploadSize(MaxUploadSizeExceededException e) {
        return BaseResponse.fail(BizException.BAD_REQUEST, "上传文件过大");
    }

    @ExceptionHandler(DataAccessException.class)
    public BaseResponse handleDataAccess(DataAccessException e) {
        log.error("数据库访问失败", e);
        return BaseResponse.fail(500, "数据库访问失败：" + e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse handleOther(Exception e) {
        log.error("未处理的异常", e);
        return BaseResponse.fail(500, "服务器内部错误：" + NestedExceptionUtils.getMostSpecificCause(e).getMessage());
    }
}
