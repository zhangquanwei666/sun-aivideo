package com.sun.aivideo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sun.aivideo.entity.ImageModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文生图模型配置 Mapper，自定义 SQL 在 resources/mapper/ImageModelConfigMapper.xml。
 */
@Mapper
public interface ImageModelConfigMapper extends BaseMapper<ImageModelConfig> {

    /** 已启用的模型，按 sort_order 排序。对应 XML：selectEnabledList */
    List<ImageModelConfig> selectEnabledList();

    /** 按模型编码查询（含已停用）。对应 XML：selectByModelCode */
    ImageModelConfig selectByModelCode(@Param("modelCode") String modelCode);
}
