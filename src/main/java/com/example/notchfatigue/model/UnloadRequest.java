package com.example.notchfatigue.model;

/**
 * 卸载迟滞薄壳层的请求：从一个已求得的加载点卸载到另一名义应力。
 *
 * @param material         材料参数（E, K, n）
 * @param kt               应力集中系数 Kt（≥ 1）
 * @param peakNominalStress 峰值（卸载起点）名义应力
 * @param targetNominalStress 卸载目标名义应力（≥ 0，通常小于峰值）
 */
public record UnloadRequest(
        MaterialProperties material,
        double kt,
        double peakNominalStress,
        double targetNominalStress
) {
}
