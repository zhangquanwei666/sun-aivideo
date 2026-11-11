package com.sun.aivideo.model.vo;

import com.sun.aivideo.common.base.BaseResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * 出参：可选的视频模型列表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ModelListVO extends BaseResponse {

    private List<ModelConfigVO> models;
}
