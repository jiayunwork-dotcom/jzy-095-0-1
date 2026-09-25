package com.example.notchfatigue.constitutive;

import com.example.notchfatigue.model.MaterialProperties;

/**
 * Ramberg–Osgood 单调本构（纯数学，无 I/O，无求根）。
 *
 * <p>总应变：ε = σ/E + (σ/K)^(1/n)
 *
 * <p>核心约定：塑性项的幂次是 <b>1/n</b>，绝不能写成 n。
 * n 通常在 0.1~0.3 之间，两种写法方向相反，写错会让整个塑性段全歪。
 */
public final class RambergOsgood {

    /** 塑性应变相对弹性应变低于该比例时，视为仍在弹性范围（ε ≈ σ/E）。 */
    public static final double PLASTIC_RATIO_LIMIT = 1.0e-6;

    private final double e;
    private final double k;
    private final double n;
    private final double inverseN;

    public RambergOsgood(MaterialProperties material) {
        this(material.e(), material.k(), material.n());
    }

    public RambergOsgood(double e, double k, double n) {
        // 合法性主要由 InputValidator 负责；这里做最小防御，避免 NaN 污染后续数值。
        if (!Double.isFinite(e) || e <= 0.0) {
            throw new IllegalArgumentException("弹性模量 E 必须为正，收到: " + e);
        }
        if (!Double.isFinite(k) || k <= 0.0) {
            throw new IllegalArgumentException("强度系数 K 必须为正，收到: " + k);
        }
        if (!Double.isFinite(n) || n <= 0.0) {
            throw new IllegalArgumentException("硬化指数 n 必须为正，收到: " + n);
        }
        this.e = e;
        this.k = k;
        this.n = n;
        this.inverseN = 1.0 / n;
    }

    /** 弹性应变分量 σ/E。 */
    public double elasticStrain(double stress) {
        return stress / e;
    }

    /**
     * 塑性应变分量 (σ/K)^(1/n)。
     * 单调核算应力非负；σ = 0 时塑性应变为 0。
     */
    public double plasticStrain(double stress) {
        if (stress <= 0.0) {
            return 0.0;
        }
        return Math.pow(stress / k, inverseN);
    }

    /** 总应变 ε = σ/E + (σ/K)^(1/n)。 */
    public double totalStrain(double stress) {
        return elasticStrain(stress) + plasticStrain(stress);
    }

    /**
     * 判断给定应力水平是否仍可视为纯弹性：塑性应变与弹性应变之比不超过
     * {@link #PLASTIC_RATIO_LIMIT}。弹性范围内直接走 σ = Kt·σn 的精确解，
     * 不进求根器，保证“弹性极限内 σ = Kt·σn”严格成立。
     */
    public boolean isEffectivelyElastic(double stress) {
        if (stress <= 0.0) {
            return true;
        }
        double elastic = stress / e;
        double plastic = plasticStrain(stress);
        return plastic <= PLASTIC_RATIO_LIMIT * elastic;
    }

    /**
     * Masing 卸载分支的应变变程：
     * Δε = Δσ/E + 2·(Δσ/(2K))^(1/n)。
     *
     * <p>这是“很薄一层”的卸载迟滞估计：分支形状是单调曲线的 2 倍放大，
     * 不追踪完整迟滞回线、不考虑循环硬化/软化。
     */
    public double hysteresisStrainRange(double deltaStress) {
        if (deltaStress <= 0.0) {
            return 0.0;
        }
        return deltaStress / e + 2.0 * Math.pow(deltaStress / (2.0 * k), inverseN);
    }

    /** Masing 卸载分支塑性部分：2·(Δσ/(2K))^(1/n)。 */
    public double hysteresisPlasticRange(double deltaStress) {
        if (deltaStress <= 0.0) {
            return 0.0;
        }
        return 2.0 * Math.pow(deltaStress / (2.0 * k), inverseN);
    }

    public double getE() {
        return e;
    }

    public double getK() {
        return k;
    }

    public double getN() {
        return n;
    }
}
