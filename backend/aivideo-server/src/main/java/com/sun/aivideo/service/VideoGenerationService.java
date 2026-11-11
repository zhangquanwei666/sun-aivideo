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

    /** 按 ID 批量查询（已删除的不返回），生成记录列表使用 */
    java.util.Map<Long, VideoTaskVO> listByIds(java.util.Collection<Long> ids);

    PageVO<VideoTaskVO> page(VideoTaskQueryDTO query);

    /** 费用与耗时汇总（包含已删除的任务） */
    VideoStatsVO stats();

    /**
     * 删除视频（逻辑删除）：只能删除已结束的任务（成功 / 失败 / 取消 / 超时），
     * 删除后列表与详情不再返回，数据和本地视频文件保留。
     */
    void delete(Long id);
}
