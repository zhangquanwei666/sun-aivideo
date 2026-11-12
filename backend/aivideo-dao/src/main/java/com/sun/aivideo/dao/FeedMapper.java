package com.sun.aivideo.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sun.aivideo.dao.bo.FeedItemBO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 生成记录 Mapper：把视频任务和图片任务合并成一个按时间倒序的列表，
 * 只返回类型与 ID，详情再分别按 ID 批量查询。SQL 在 resources/mapper/FeedMapper.xml。
 */
@Mapper
public interface FeedMapper {

    /**
     * 分页查询视频 + 图片（不含已删除）。对应 XML：selectFeedPage
     *
     * @param userId   只查该用户的记录；为空时查全部用户（管理后台使用）
     * @param feedType VIDEO / IMAGE，为空时两种都查
     * @param keyword  按提示词模糊搜索，可为空
     */
    IPage<FeedItemBO> selectFeedPage(IPage<FeedItemBO> page,
                                     @Param("userId") Long userId,
                                     @Param("feedType") String feedType,
                                     @Param("keyword") String keyword);
}
