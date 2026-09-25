package com.example.notchfatigue.model;

/**
 * 单点核算请求。
 *
 * @param material 材料参数（E, K, n）
 * @param kt       应力集中系数 Kt（≥ 1）
 * @param nominalStress 名义应力 σn（≥ 0，拉正压负的单调核算取非负值）
 */
public record NotchRequest(MaterialProperties material, double kt, double nominalStress) {
}
