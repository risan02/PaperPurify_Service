package com.sunnyday.lychat.entity;

import lombok.Data;

/**
 * 质量维度值对象
 */
@Data
public class QualityDimensionVo {
    /**
     * 维度名称
     */
    private String name;

    /**
     * 分数 (0-100)
     */
    private Integer score;

    /**
     * 评价
     */
    private String evaluation;
}
