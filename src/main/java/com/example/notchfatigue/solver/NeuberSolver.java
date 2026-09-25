package com.example.notchfatigue.solver;

import com.example.notchfatigue.constitutive.RambergOsgood;
import com.example.notchfatigue.model.MaterialProperties;
import com.example.notchfatigue.model.NotchResult;

/**
 * Neuber 双曲线与 Ramberg–Osgood 联立求根（核心计算内核）。
 *
 * <p>Neuber 关系（弹性名义应力 → 缺口根真实应力/应变）：
 * <pre>
 *     σ·ε = (Kt·σn)² / E
 * </pre>
 * 代入 RO 本构 ε = σ/E + (σ/K)^(1/n)，得到关于 σ 的标量方程：
 * <pre>
 *     f(σ) = σ²/E + σ·(σ/K)^(1/n) − (Kt·σn)²/E = 0
 * </pre>
 * f 关于 σ ≥ 0 严格单调递增，根唯一。
 *
 * <p>根的位置：弹性外推应力 σe = Kt·σn 处
 * f(σe) = σe·(σe/K)^(1/n) ≥ 0，故真实应力 σ ∈ [0, σe]。
 * 进入塑性后 σ &lt; Kt·σn——绝不能继续沿用 σ = Kt·σn，
 * 否则塑性应变不被放大，Neuber 的交叉规则整体失效。
 *
 * <p>Kt = 1（无缺口）时按需求约定退化为单轴 Ramberg–Osgood：
 * 局部应力就是名义应力 σ = σn，局部应变 ε = RO(σn)，不再走能量联立。
 * Kt &gt; 1 才使用 Neuber 联立根。
 */
public final class NeuberSolver {

    private final BisectionRootFinder rootFinder;

    public NeuberSolver() {
        this(new BisectionRootFinder());
    }

    public NeuberSolver(BisectionRootFinder rootFinder) {
        this.rootFinder = rootFinder;
    }

    /**
     * 单点核算。
     *
     * @param material     材料参数（E, K, n，校验由上层负责）
     * @param kt           应力集中系数（≥ 1）
     * @param nominalStress 名义应力 σn（≥ 0）
     * @return 弹塑性结果与弹性对照
     * @throws NonConvergenceException 求根不收敛（调用方必须按错误返回）
     */
    public NotchResult solve(MaterialProperties material, double kt, double nominalStress) {
        RambergOsgood ro = new RambergOsgood(material);

        final double elasticStress = kt * nominalStress;          // σe = Kt·σn
        final double elasticStrain = elasticStress / ro.getE();   // 弹性对照应变

        // Kt = 1：无缺口，Neuber 退化为单轴 Ramberg–Osgood。
        // 真实应力 = 名义应力；总应变直接由 RO 给出，不需要求根。
        if (kt == 1.0) {
            if (ro.isEffectivelyElastic(nominalStress)) {
                return new NotchResult(
                        nominalStress, nominalStress, elasticStrain,
                        0.0, nominalStress, elasticStrain, false);
            }
            double plasticStrain = ro.plasticStrain(nominalStress);
            return new NotchResult(
                    nominalStress,
                    nominalStress,
                    nominalStress / ro.getE() + plasticStrain,
                    plasticStrain,
                    nominalStress,
                    nominalStress / ro.getE(),
                    true);
        }

        final double energyTarget = elasticStress * elasticStress / ro.getE(); // C = σe²/E

        // 弹性范围：RO 在 σe 处没有可观塑性项时，解就是精确的 σ = σe、ε = σe/E
        if (ro.isEffectivelyElastic(elasticStress)) {
            return new NotchResult(
                    nominalStress,
                    elasticStress,
                    elasticStrain,
                    0.0,
                    elasticStress,
                    elasticStrain,
                    false);
        }

        // 进入塑性：联立求根。σ ∈ (0, σe)
        final double e = ro.getE();
        final double k = ro.getK();
        final double inverseN = 1.0 / ro.getN();

        final double lower = 0.0;
        final double upper = elasticStress;

        double localStress = rootFinder.findRoot(sigma -> {
            double plastic = sigma <= 0.0 ? 0.0 : Math.pow(sigma / k, inverseN);
            double energy = sigma * (sigma / e + plastic);
            return energy - energyTarget;
        }, lower, upper);

        double plasticStrain = ro.plasticStrain(localStress);
        double localStrain = localStress / e + plasticStrain;

        return new NotchResult(
                nominalStress,
                localStress,
                localStrain,
                plasticStrain,
                elasticStress,
                elasticStrain,
                true);
    }
}
