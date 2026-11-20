package com.sun.aivideo.dao;

import com.sun.aivideo.entity.ModelConfig;
import com.sun.aivideo.entity.ModelType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 图片与视频模型配置 Mapper。
 * <p>自定义查询的 SQL 统一写在 resources/mapper/ModelConfigMapper.xml，方法名与 XML 中的 id 一一对应。</p>
 */
@Mapper
public interface ModelConfigMapper {

    /**
     * 已启用的模型，按 sort_order 排序。
     * 对应 XML：selectEnabledList
     */
    List<ModelConfig> selectEnabledList(@Param("modelType") ModelType modelType);

    /**
     * 按模型编码查询（含已停用）。
     * 对应 XML：selectByModelCode
     */
    ModelConfig selectByModelCode(@Param("modelType") ModelType modelType, @Param("modelCode") String modelCode);

    /** 查询指定类型的全部模型，用于显示含停用模型的历史任务与统计。 */
    List<ModelConfig> selectAllByType(@Param("modelType") ModelType modelType);
}
