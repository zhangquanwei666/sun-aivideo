package com.sun.aivideo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sun.aivideo.entity.ModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 视频模型配置 Mapper。
 * <p>自定义查询的 SQL 统一写在 resources/mapper/ModelConfigMapper.xml，方法名与 XML 中的 id 一一对应。</p>
 */
@Mapper
public interface ModelConfigMapper extends BaseMapper<ModelConfig> {

    /**
     * 已启用的模型，按 sort_order 排序。
     * 对应 XML：selectEnabledList
     */
    List<ModelConfig> selectEnabledList();

    /**
     * 按模型编码查询（含已停用）。
     * 对应 XML：selectByModelCode
     */
    ModelConfig selectByModelCode(@Param("modelCode") String modelCode);
}
