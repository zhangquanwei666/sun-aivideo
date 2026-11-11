package com.sun.aivideo.common.base;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 所有入参的基类。
 */
@Data
public abstract class BaseRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 调用方传入的请求 ID（可选），用于链路追踪和排查问题 */
    private String requestId;
}
