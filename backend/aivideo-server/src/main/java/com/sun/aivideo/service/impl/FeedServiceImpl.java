package com.sun.aivideo.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sun.aivideo.dao.FeedMapper;
import com.sun.aivideo.dao.bo.FeedItemBO;
import com.sun.aivideo.model.dto.FeedQueryDTO;
import com.sun.aivideo.model.vo.FeedItemVO;
import com.sun.aivideo.model.vo.ImageTaskVO;
import com.sun.aivideo.model.vo.PageVO;
import com.sun.aivideo.model.vo.VideoTaskVO;
import com.sun.aivideo.service.FeedService;
import com.sun.aivideo.service.ImageGenerationService;
import com.sun.aivideo.service.VideoGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private static final String VIDEO = "VIDEO";

    private final FeedMapper feedMapper;
    private final VideoGenerationService videoGenerationService;
    private final ImageGenerationService imageGenerationService;

    @Override
    public PageVO<FeedItemVO> page(FeedQueryDTO query) {
        IPage<FeedItemBO> page = feedMapper.selectFeedPage(Page.of(query.getPageNum(), query.getPageSize()));

        // 先按类型拿到 ID，再分别批量查询详情，最后按原顺序拼回去
        List<Long> videoIds = new ArrayList<>();
        List<Long> imageIds = new ArrayList<>();
        for (FeedItemBO item : page.getRecords()) {
            (VIDEO.equals(item.getFeedType()) ? videoIds : imageIds).add(item.getId());
        }
        Map<Long, VideoTaskVO> videos = videoGenerationService.listByIds(videoIds);
        Map<Long, ImageTaskVO> images = imageGenerationService.listByIds(imageIds);

        List<FeedItemVO> records = page.getRecords().stream()
                .map(item -> VIDEO.equals(item.getFeedType())
                        ? (videos.containsKey(item.getId()) ? FeedItemVO.ofVideo(videos.get(item.getId())) : null)
                        : (images.containsKey(item.getId()) ? FeedItemVO.ofImage(images.get(item.getId())) : null))
                .filter(Objects::nonNull)
                .toList();
        return new PageVO<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }
}
