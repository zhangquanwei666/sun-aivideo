package com.sun.aivideo.model.dto;

import com.sun.aivideo.common.base.BasePageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 入参：分页查询视频任务（pageNum / pageSize 继承自 BasePageRequest）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VideoTaskQueryDTO extends BasePageRequest {

    private String status;

    private String modelCode;

    /** 按提示词模糊搜索 */
    private String keyword;
}
