package com.sun.aivideo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sun.aivideo.dao.bo.ModelStatBO;
import com.sun.aivideo.dao.bo.VideoStatsBO;
import com.sun.aivideo.entity.VideoTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 视频任务 Mapper。
 * <p>单表增删改、按主键查询使用 MyBatis-Plus {@link BaseMapper} 自带方法；
 * 自定义查询的 SQL 统一写在 resources/mapper/VideoTaskMapper.xml，方法名与 XML 中的 id 一一对应。</p>
 */
@Mapper
public interface VideoTaskMapper extends BaseMapper<VideoTask> {

    /**
     * 分页查询任务（MyBatis-Plus 分页插件自动追加 count 与 limit）。
     * 对应 XML：selectTaskPage
     */
    IPage<VideoTask> selectTaskPage(IPage<VideoTask> page,
                                    @Param("status") String status,
                                    @Param("modelCode") String modelCode,
                                    @Param("keyword") String keyword);

    /**
     * 查询需要轮询厂商状态的任务（指定状态且已拿到厂商任务 ID）。
     * 对应 XML：selectActiveTasks
     */
    List<VideoTask> selectActiveTasks(@Param("statuses") Collection<String> statuses);

    /**
     * 费用与耗时汇总。
     * 对应 XML：selectStats
     */
    VideoStatsBO selectStats();

    /**
     * 按模型分组的费用与耗时统计。
     * 对应 XML：selectStatsByModel
     */
    List<ModelStatBO> selectStatsByModel();
}
