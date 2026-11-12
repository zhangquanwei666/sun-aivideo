package com.sun.aivideo.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sun.aivideo.admin.model.AdminFeedItem;
import com.sun.aivideo.admin.model.PageResponse;
import com.sun.aivideo.common.enums.ErrorCode;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.dao.FeedMapper;
import com.sun.aivideo.dao.ImageTaskMapper;
import com.sun.aivideo.dao.UserMapper;
import com.sun.aivideo.dao.VideoTaskMapper;
import com.sun.aivideo.dao.bo.FeedItemBO;
import com.sun.aivideo.entity.ImageTask;
import com.sun.aivideo.entity.User;
import com.sun.aivideo.entity.VideoTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminFeedService {

    private final FeedMapper feedMapper;
    private final VideoTaskMapper videoTaskMapper;
    private final ImageTaskMapper imageTaskMapper;
    private final UserMapper userMapper;

    public PageResponse<AdminFeedItem> page(long pageNum, long pageSize, Long userId,
                                            String feedType, String keyword) {
        String type = normalizeType(feedType);
        var page = feedMapper.selectFeedPage(Page.of(pageNum, pageSize), userId, type, trim(keyword));

        var videoIds = page.getRecords().stream()
                .filter(item -> "VIDEO".equals(item.getFeedType()))
                .map(FeedItemBO::getId).toList();
        var imageIds = page.getRecords().stream()
                .filter(item -> "IMAGE".equals(item.getFeedType()))
                .map(FeedItemBO::getId).toList();
        var userIds = page.getRecords().stream().map(FeedItemBO::getUserId).filter(java.util.Objects::nonNull).toList();

        Map<Long, VideoTask> videos = videoIds.isEmpty() ? Map.of()
                : byId(videoTaskMapper.selectBatchIds(videoIds), VideoTask::getId);
        Map<Long, ImageTask> images = imageIds.isEmpty() ? Map.of()
                : byId(imageTaskMapper.selectBatchIds(imageIds), ImageTask::getId);
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : byId(userMapper.selectBatchIds(userIds), User::getId);

        var records = page.getRecords().stream().map(item -> {
            User user = users.get(item.getUserId());
            Object task = "VIDEO".equals(item.getFeedType()) ? videos.get(item.getId()) : images.get(item.getId());
            return AdminFeedItem.builder()
                    .type(item.getFeedType())
                    .id(item.getId())
                    .userId(item.getUserId())
                    .username(user == null ? null : user.getUsername())
                    .createdAt(item.getCreatedAt())
                    .task(task)
                    .build();
        }).toList();
        return PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    private static String normalizeType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String type = value.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"VIDEO".equals(type) && !"IMAGE".equals(type)) {
            throw new BizException(ErrorCode.INVALID_FEED_TYPE);
        }
        return type;
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static <T> Map<Long, T> byId(Collection<T> values, Function<T, Long> idGetter) {
        return values.stream().collect(Collectors.toMap(idGetter, Function.identity(), (left, right) -> left));
    }
}
