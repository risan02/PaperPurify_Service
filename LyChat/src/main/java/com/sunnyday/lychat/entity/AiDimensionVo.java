package com.sunnyday.lychat.entity;

import lombok.Data;

/**
 * AI分析维度值对象
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
     */
    private String evaluation;
}
