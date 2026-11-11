package com.sun.aivideo.dao.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生成记录的一行：只含类型与 ID。
 */
@Data
public class FeedItemBO {

    /** VIDEO / IMAGE */
    private String feedType;
    private Long id;
    private LocalDateTime createdAt;
}
