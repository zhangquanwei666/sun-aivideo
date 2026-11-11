package com.sun.aivideo.common.base;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;

/**
 * 分页查询入参的基类。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class BasePageRequest extends BaseRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码从 1 开始")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 100, message = "每页最多 100 条")
    private Integer pageSize = 20;
}
