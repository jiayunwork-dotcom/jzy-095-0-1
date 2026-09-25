package com.example.notchfatigue.solver;

import com.example.notchfatigue.model.MaterialProperties;

/**
 * 纯弹性对照基线：σ = Kt·σn，ε = σ/E。
 *
 * <p>只做弹性外推，不含任何塑性逻辑，用来与 Neuber + RO 的弹塑性结果并列对照：
 * 名义应力超过比例极限后，真实应力应当低于这里给出的 Kt·σn。
 */
public final class ElasticReference {

    private final double e;

    public ElasticReference(MaterialProperties material) {
        this.e = material.e();
    }

    /** 弹性局部应力 Kt·σn。 */
    public double localStress(double kt, double nominalStress) {
        return kt * nominalStress;
    }

    /** 弹性局部应变 (Kt·σn)/E。 */
    public double localStrain(double kt, double nominalStress) {
        return kt * nominalStress / e;
    }
}
