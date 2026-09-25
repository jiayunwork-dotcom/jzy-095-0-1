package com.example.notchfatigue.model;

import java.util.List;

/**
 * 批量加载序列核算请求：对一串递增的名义应力逐个求根，各点互不影响。
 *
 * @param material       材料参数（E, K, n）
 * @param kt             应力集中系数 Kt（≥ 1）
 * @param nominalStresses 名义应力序列（建议递增，允许任意个数）
 */
public record SequenceRequest(MaterialProperties material, double kt, List<Double> nominalStresses) {
}
