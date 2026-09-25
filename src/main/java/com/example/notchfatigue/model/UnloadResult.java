package com.example.notchfatigue.model;

/**
 * 卸载迟滞薄壳层结果（Masing 假设）。
 *
 * <p>卸载分支按 2·K、2·σ 规则求根，不做完整的迟滞回线追踪。
 *
 * @param peakLocalStress    峰值缺口根应力
 * @param peakLocalStrain    峰值缺口根应变
 * @param targetLocalStress  卸载目标点缺口根应力
 * @param targetLocalStrain  卸载目标点缺口根应变
 * @param deltaStress        应力变程 σ_peak − σ_target
 * @param deltaStrain        应变变程 ε_peak − ε_target
 */
public record UnloadResult(
        double peakLocalStress,
        double peakLocalStrain,
        double targetLocalStress,
        double targetLocalStrain,
        double deltaStress,
        double deltaStrain
) {
}
