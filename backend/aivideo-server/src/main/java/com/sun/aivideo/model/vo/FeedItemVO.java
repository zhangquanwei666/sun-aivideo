package com.sun.aivideo.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成记录中的一项：视频或图片二选一。作为分页结果的列表元素返回，不单独返回。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedItemVO {

    /** VIDEO / IMAGE */
    private String type;
    private VideoTaskVO video;
    private ImageTaskVO image;

    public static FeedItemVO ofVideo(VideoTaskVO video) {
        return new FeedItemVO("VIDEO", video, null);
    }

    public static FeedItemVO ofImage(ImageTaskVO image) {
        return new FeedItemVO("IMAGE", null, image);
    }
}
