package com.example.notchfatigue.solver;

import com.example.notchfatigue.constitutive.RambergOsgood;
import com.example.notchfatigue.model.MaterialProperties;
import com.example.notchfatigue.model.NotchResult;
import com.example.notchfatigue.model.UnloadResult;

/**
 * 卸载迟滞的“很薄一层”：基于 Masing 假设的卸载分支估计。
 *
 * <p>从加载峰值点 (σ_a, ε_a) 卸载名义应力变程 Δσn 时，缺口根局部量的变程满足：
 * <pre>
 *     分支本构（2 倍放大 RO）：Δε = Δσ/E + 2·(Δσ/(2K))^(1/n)
 *     Neuber 变程形式：       Δσ·Δε = (Kt·Δσn)²/E
 * </pre>
 * 联立对 Δσ 求根（与单调 Neuber 求根同构，只是应变律换成 Masing 分支），
 * 卸载目标点为 σ_t = σ_a − Δσ、ε_t = ε_a − Δε。
 *
 * <p>明确不做的事：完整迟滞回线追踪、记忆抹除、循环硬化/软化、平均应力松弛。
 */
public final class NeuberUnloader {

    private final BisectionRootFinder rootFinder;
    private final NeuberSolver neuberSolver;

    public NeuberUnloader() {
        this(new BisectionRootFinder(), new NeuberSolver());
    }

    public NeuberUnloader(BisectionRootFinder rootFinder, NeuberSolver neuberSolver) {
        this.rootFinder = rootFinder;
        this.neuberSolver = neuberSolver;
    }

    /**
     * 计算从峰值名义应力卸载到目标名义应力的分支结果。
     */
    public UnloadResult unload(MaterialProperties material, double kt,
                               double peakNominalStress, double targetNominalStress) {
        RambergOsgood ro = new RambergOsgood(material);

        // 峰值点与目标点的单调加载解（各点独立求解）
        NotchResult peak = neuberSolver.solve(material, kt, peakNominalStress);

        double deltaNominal = peakNominalStress - targetNominalStress; // ≥ 0
        double elasticDeltaStress = kt * deltaNominal;                 // Δσ 的弹性上界
        double energyTarget = elasticDeltaStress * elasticDeltaStress / ro.getE();

        final double e = ro.getE();
        final double k = ro.getK();
        final double inverseN = 1.0 / ro.getN();

        double deltaStress;
        double deltaStrain;

        if (deltaNominal == 0.0) {
            deltaStress = 0.0;
            deltaStrain = 0.0;
        } else if (ro.isEffectivelyElastic(elasticDeltaStress)) {
            // 变程完全弹性：Δσ = Kt·Δσn，Δε = Δσ/E
            deltaStress = elasticDeltaStress;
            deltaStrain = deltaStress / e;
        } else {
            // Masing 分支 + Neuber 变程联立求根，Δσ ∈ (0, Kt·Δσn)
            deltaStress = rootFinder.findRoot(ds -> {
                double plastic = ds <= 0.0 ? 0.0 : 2.0 * Math.pow(ds / (2.0 * k), inverseN);
                return ds * (ds / e + plastic) - energyTarget;
            }, 0.0, elasticDeltaStress);
            deltaStrain = ro.hysteresisStrainRange(deltaStress);
        }

        return new UnloadResult(
                peak.localStress(),
                peak.localStrain(),
                peak.localStress() - deltaStress,
                peak.localStrain() - deltaStrain,
                deltaStress,
                deltaStrain);
    }
}
