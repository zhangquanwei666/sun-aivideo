package com.sun.aivideo.admin.handler;

import cn.dev33.satoken.exception.NotLoginException;
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

@Slf4j
@RestControllerAdvice
public class AdminExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public BaseResponse handleNotLogin(NotLoginException e) {
        return BaseResponse.fail(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(BizException.class)
    public BaseResponse handleBusiness(BizException e) {
        return BaseResponse.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({BindException.class, ConstraintViolationException.class})
    public BaseResponse handleValidation(Exception e) {
        return BaseResponse.fail(ErrorCode.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse handleMalformed(HttpMessageNotReadableException e) {
        return BaseResponse.fail(ErrorCode.MALFORMED_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    public BaseResponse handleDatabase(DataAccessException e) {
        log.error("管理服务数据库访问失败", e);
        return BaseResponse.fail(ErrorCode.DATABASE_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse handleOther(Exception e) {
        log.error("管理服务未处理异常", e);
        return BaseResponse.fail(ErrorCode.INTERNAL_ERROR);
    }
}
