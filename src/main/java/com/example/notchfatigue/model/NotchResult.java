package com.example.notchfatigue.model;

/**
 * 单点核算结果：弹塑性解与纯弹性外推对照一并给出。
 *
 * @param nominalStress        名义应力 σn
 * @param localStress          Neuber + Ramberg–Osgood 联立求得的缺口根真实应力 σ
 * @param localStrain          缺口根真实总应变 ε
 * @param plasticStrain        塑性应变分量 (σ/K)^(1/n)
 * @param elasticStress        弹性对照应力 Kt·σn
 * @param elasticStrain        弹性对照应变 (Kt·σn)/E
 * @param plastic              该点是否进入塑性（true 表示由 Neuber 联立求根得到）
 */
public record NotchResult(
        double nominalStress,
        double localStress,
        double localStrain,
        double plasticStrain,
        double elasticStress,
        double elasticStrain,
        boolean plastic
) {
}
