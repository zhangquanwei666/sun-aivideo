package com.sun.aivideo.model.dto;

import com.sun.aivideo.common.base.BasePageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 入参：分页查询生成记录（视频与图片合并，pageNum / pageSize 继承自 BasePageRequest）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FeedQueryDTO extends BasePageRequest {
}
