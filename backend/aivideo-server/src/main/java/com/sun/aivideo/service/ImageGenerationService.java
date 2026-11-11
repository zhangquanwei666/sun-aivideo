package com.sun.aivideo.service;

import com.sun.aivideo.model.dto.ImageGenerateDTO;
import com.sun.aivideo.model.vo.ImageStatsVO;
import com.sun.aivideo.model.vo.ImageTaskVO;

import java.util.Collection;
import java.util.Map;

public interface ImageGenerationService {

    /** 创建文生图任务：立即返回，润色与生成在后台进行 */
    ImageTaskVO create(ImageGenerateDTO dto);

    ImageTaskVO detail(Long id);

    /** 按 ID 批量查询（已删除的不返回），生成记录列表使用 */
    Map<Long, ImageTaskVO> listByIds(Collection<Long> ids);

    /** 逻辑删除，只能删除已结束的任务 */
    void delete(Long id);

    /** 费用与耗时汇总（包含已删除的任务） */
    ImageStatsVO stats();
}
