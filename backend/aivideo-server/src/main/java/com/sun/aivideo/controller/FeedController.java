package com.sun.aivideo.controller;

import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.model.dto.FeedQueryDTO;
import com.sun.aivideo.model.vo.FeedItemVO;
import com.sun.aivideo.model.vo.PageVO;
import com.sun.aivideo.service.FeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /** 生成记录：视频与图片合并，按时间倒序分页（不含已删除） */
    @GetMapping
    public PageVO<FeedItemVO> page(@Valid FeedQueryDTO query) {
        return BaseResponse.ok(feedService.page(query));
    }
}
