package com.example.notchfatigue.model;

import java.util.List;

/**
 * 批量加载序列核算结果，勾勒从弹性进入塑性的整段响应。
 */
public record SequenceResult(
        double kt,
        MaterialProperties material,
        List<NotchResult> points
) {
}
