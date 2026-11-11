package com.sun.aivideo.model.dto;

import com.sun.aivideo.common.base.BaseRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 入参：查询 / 删除文生图任务（id 由路径 /api/images/{id} 绑定）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ImageIdDTO extends BaseRequest {

    @NotNull(message = "任务 ID 不能为空")
    @Positive(message = "任务 ID 不合法")
    private Long id;
}
