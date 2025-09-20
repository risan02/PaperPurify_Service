package com.sunnyday.lychat.entity;

import lombok.Data;
import java.util.List;

/**
 * AI分析结果值对象
 */
@Data
public class AiAnalysisResultVo {
    /**
     * AI生成可能性分数（AI生成痕迹明显程度的数值，百分制，0-100，分数越高说明由AI生成的痕迹越明显）
     */
    private Integer aiScore;

    /**
     * AI痕迹分析维度
     * 维度名有三个：言語的困惑度、構造とテンプレート使用傾向、専門用語密度と論理的一貫性。
     * 每个AiDimensionVo对象包括维度名、该维度名下AI生成痕迹的主观评价（高、中、低）、AI生成痕迹在该维度的解释说明（尽量2句话的篇幅）
     */
    private List<AiDimensionVo> aiDimensions;

    /**
     * 质量维度
     * 维度名有六个：志愿动机的明确性与具体性、学习计划与未来目标的合理性、表达力与说服力、与院系专业的契合度、文章结构与逻辑展开、语法与日语的准确性
     * 很对每个QualityDimensionVo对象包括：维度名、该维度下的优秀度打分和该维度下的评语说明（2句话的篇幅）
     */
    private List<QualityDimensionVo> qualityDimensions;

    /**
     * 修改建议
     * 首先给出完整的修改建议，然后给出修改后的完整材料
     */
    private List<String> recommendations;
}