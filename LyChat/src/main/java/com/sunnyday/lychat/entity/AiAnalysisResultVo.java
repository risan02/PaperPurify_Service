package com.sunnyday.lychat.entity;

import lombok.Data;
import java.util.List;

/**
 * AI分析结果值对象
 */
@Data
public class AiAnalysisResultVo {
    /**
     * AI生成可能性分数
     */
    private Integer aiScore;

    /**
     * AI生成可能性级别 (low, medium, high)
     */
    private String aiProbability;

    /**
     * AI分析维度
     */
    private List<AiDimensionVo> aiDimensions;

    /**
     * 质量维度
     */
    private List<QualityDimensionVo> qualityDimensions;

    /**
     * 修改建议
     */
    private List<String> recommendations;
}