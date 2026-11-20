package com.sun.aivideo.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sun.aivideo.dao.bo.ImageModelStatBO;
import com.sun.aivideo.dao.bo.ImageStatsBO;
import com.sun.aivideo.dao.bo.ModelStatBO;
import com.sun.aivideo.dao.bo.VideoStatsBO;
import com.sun.aivideo.entity.GenerationTask;
import com.sun.aivideo.entity.TaskType;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/** 任务 ID 在类型内唯一，所有按 ID 读写均须传入类型。 */
public interface GenerationTaskMapper {
    int insert(@Param("type") TaskType type, @Param("task") GenerationTask task);

    int updateById(@Param("type") TaskType type, @Param("task") GenerationTask task);

    GenerationTask selectById(@Param("type") TaskType type, @Param("id") Long id);

    List<GenerationTask> selectBatchIds(@Param("type") TaskType type, @Param("ids") Collection<Long> ids);

    int softDeleteById(@Param("type") TaskType type, @Param("id") Long id, @Param("userId") Long userId);

    IPage<GenerationTask> selectVideoPage(IPage<GenerationTask> page,
                                         @Param("userId") Long userId,
                                         @Param("status") String status,
                                         @Param("modelCode") String modelCode,
                                         @Param("keyword") String keyword);

    List<GenerationTask> selectActiveVideos(@Param("statuses") Collection<String> statuses);

    int failStaleVideoSubmissions(@Param("timeoutMinutes") int timeoutMinutes);

    int failUnfinishedImages(@Param("errorMessage") String errorMessage);

    VideoStatsBO selectVideoStats(@Param("userId") Long userId);

    List<ModelStatBO> selectVideoStatsByModel(@Param("userId") Long userId);

    ImageStatsBO selectImageStats(@Param("userId") Long userId);

    List<ImageModelStatBO> selectImageStatsByModel(@Param("userId") Long userId);
}
