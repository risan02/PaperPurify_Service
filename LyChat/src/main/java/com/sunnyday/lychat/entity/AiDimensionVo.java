package com.sunnyday.lychat.entity;

import lombok.Data;

/**
 * AI生成痕迹分析维度值对象
 *
 */
@Data
public class AiDimensionVo {
    /**
     * 维度名称
     */
    private String name;

    /**
     * 级别 (高, 中, 低)
     */
    private String level;

    /**
     * 评价
     * 大约2句话的文字篇幅量
     */
    private String evaluation;
}
