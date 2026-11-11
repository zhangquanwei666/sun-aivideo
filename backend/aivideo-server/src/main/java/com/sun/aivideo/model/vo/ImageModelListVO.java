package com.sun.aivideo.model.vo;

import com.sun.aivideo.common.base.BaseResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * 出参：可选的文生图厂商与模型。前端先选厂商，再按 ImageModelConfigVO.provider 过滤出该厂商的模型。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ImageModelListVO extends BaseResponse {

    private List<ProviderVO> providers;
    private List<ImageModelConfigVO> models;
}
