package com.sun.aivideo.common.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 所有响应的基类：code = 0 表示成功，非 0 时 message 为错误原因。
 * <p>各接口的响应类继承本类，业务字段与 code / message 平铺在同一层，例如：</p>
 * <pre>{"code":0,"message":"success","url":"/files/images/xx.png","fileName":"a.png","size":1024}</pre>
 * <p>响应类作为列表元素嵌套返回时（如分页 records），不设置 code / message，序列化时自动省略。</p>
 */
@Data
public class BaseResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int SUCCESS_CODE = 0;
    public static final String SUCCESS_MESSAGE = "success";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    /** 给响应对象标记成功并返回 */
    public static <T extends BaseResponse> T ok(T response) {
        response.setCode(SUCCESS_CODE);
        response.setMessage(SUCCESS_MESSAGE);
        return response;
    }

    /** 没有业务数据的成功响应 */
    public static BaseResponse ok() {
        return ok(new BaseResponse());
    }

    /** 失败响应 */
    public static BaseResponse fail(com.sun.aivideo.common.enums.ErrorCode error, Object... args) {
        return fail(error.getCode(), error.format(args));
    }

    public static BaseResponse fail(int code, String message) {
        BaseResponse response = new BaseResponse();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return code != null && code == SUCCESS_CODE;
    }
}
