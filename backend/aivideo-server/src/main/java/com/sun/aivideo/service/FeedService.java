package com.sun.aivideo.service;

import com.sun.aivideo.model.dto.FeedQueryDTO;
import com.sun.aivideo.model.vo.FeedItemVO;
import com.sun.aivideo.model.vo.PageVO;

/**
 * 生成记录：视频与图片合并，按创建时间倒序分页。
 */
public interface FeedService {

    PageVO<FeedItemVO> page(FeedQueryDTO query);
}
