package com.sun.aivideo.service;

import com.sun.aivideo.model.dto.VideoGenerateDTO;
import com.sun.aivideo.model.dto.VideoTaskQueryDTO;
import com.sun.aivideo.model.vo.PageVO;
import com.sun.aivideo.model.vo.VideoStatsVO;
import com.sun.aivideo.model.vo.VideoTaskVO;

public interface VideoGenerationService {

    /** 创建生成任务：立即返回，润色与提交在后台进行 */
    VideoTaskVO create(VideoGenerateDTO dto);

    VideoTaskVO detail(Long id);

    PageVO<VideoTaskVO> page(VideoTaskQueryDTO query);

    /** 费用与耗时汇总 */
    VideoStatsVO stats();
}
