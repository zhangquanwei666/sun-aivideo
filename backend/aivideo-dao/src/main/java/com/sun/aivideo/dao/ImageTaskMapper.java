package com.sun.aivideo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sun.aivideo.dao.bo.ImageModelStatBO;
import com.sun.aivideo.dao.bo.ImageStatsBO;
import com.sun.aivideo.entity.ImageTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文生图任务 Mapper。
 * 单表增删改、按主键查询用 BaseMapper 自带方法；自定义 SQL 在 resources/mapper/ImageTaskMapper.xml。
 */
@Mapper
public interface ImageTaskMapper extends BaseMapper<ImageTask> {

    /** 逻辑删除，返回影响行数。对应 XML：softDeleteById */
    int softDeleteById(@Param("id") Long id);

    /**
     * 把未完成的任务标记为失败（服务重启时调用，避免任务一直停在"生成中"）。
     * 对应 XML：failUnfinished
     */
    int failUnfinished(@Param("errorMessage") String errorMessage);

    /** 费用与耗时汇总（包含已删除）。对应 XML：selectStats */
    ImageStatsBO selectStats();

    /** 按模型分组统计（包含已删除）。对应 XML：selectStatsByModel */
    List<ImageModelStatBO> selectStatsByModel();
}
