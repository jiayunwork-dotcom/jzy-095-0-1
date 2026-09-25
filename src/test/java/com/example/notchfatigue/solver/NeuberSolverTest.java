package com.example.notchfatigue.solver;

import com.example.notchfatigue.model.MaterialProperties;
import com.example.notchfatigue.model.NotchResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Neuber + Ramberg–Osgood 联立求根测试。
 *
 * 重点：
 * 1) 弹性极限内 σ = Kt·σn、ε = σ/E 严格成立；
 * 2) Kt = 1 退回单轴 Ramberg–Osgood；
 * 3) 进入塑性后真实应力 &lt; Kt·σn、真实应变 &gt; 弹性应变；
 * 4) 求根不收敛走错误路径；
 * 5) 结果必须同时满足 Neuber 恒等式与 RO 本构。
 */
class NeuberSolverTest {

    private static final MaterialProperties STEEL =
            new MaterialProperties(206000.0, 1200.0, 0.2);

    private final NeuberSolver solver = new NeuberSolver(new BisectionRootFinder());

    @Test
    void zeroNominalStressGivesZero() {
        NotchResult r = solver.solve(STEEL, 3.0, 0.0);
        assertEquals(0.0, r.localStress(), 0.0);
        assertEquals(0.0, r.localStrain(), 0.0);
        assertFalse(r.plastic());
    }

    @Test
    void elasticRangeExactlyEqualsKtTimesNominal() {
        // 小名义应力：Kt·σn = 3，塑性项 ~4e-16，严格落在弹性范围
        NotchResult r = solver.solve(STEEL, 3.0, 1.0);
        assertEquals(3.0, r.localStress(), 1e-12);
        assertEquals(3.0 / 206000.0, r.localStrain(), 1e-15);
        assertEquals(0.0, r.plasticStrain(), 0.0);
        assertFalse(r.plastic());
        assertEquals(3.0, r.elasticStress(), 0.0);
    }

    @Test
    void ktEqualsOneDegradesToUniaxialRambergOsgood() {
        // 弹性段
        NotchResult elastic = solver.solve(STEEL, 1.0, 5.0);
        assertEquals(5.0, elastic.localStress(), 1e-12);
        assertEquals(5.0 / 206000.0, elastic.localStrain(), 1e-15);

        // 塑性段：σ = σn，ε = σ/E + (σ/K)^(1/n)，直接本构给出而不是能量根
        double nominal = 300.0;
        NotchResult plastic = solver.solve(STEEL, 1.0, nominal);
        assertEquals(nominal, plastic.localStress(), 1e-9);
        double expectedStrain = nominal / 206000.0 + Math.pow(nominal / 1200.0, 5.0);
        assertEquals(expectedStrain, plastic.localStrain(), 1e-10);
        assertTrue(plastic.plastic());
    }

    @Test
    void plasticLocalStressBelowElasticExtrapolation() {
        // 名义应力足够大：σn = 200，Kt·σn = 600
        NotchResult r = solver.solve(STEEL, 3.0, 200.0);
        assertTrue(r.plastic());
        assertTrue(r.localStress() < 600.0,
                "塑性后真实应力应低于弹性外推 Kt·σn，实际: " + r.localStress());
        // 真实应变高于弹性应变（应变被放大）
        assertTrue(r.localStrain() > r.elasticStrain(),
                "塑性后真实应变应高于弹性应变");
        // 对照基线不变
        assertEquals(600.0, r.elasticStress(), 0.0);
        assertEquals(600.0 / 206000.0, r.elasticStrain(), 1e-15);
    }

    @Test
    void solutionSatisfiesBothNeuberIdentityAndConstitutiveLaw() {
        double[] nominalStresses = {50.0, 120.0, 200.0, 300.0, 400.0};
        for (double sn : nominalStresses) {
            NotchResult r = solver.solve(STEEL, 3.0, sn);
            double s = r.localStress();
            double epsilon = r.localStrain();

            // RO 本构：ε = σ/E + (σ/K)^(1/n)
            double roStrain = s / 206000.0 + Math.pow(s / 1200.0, 5.0);
            assertEquals(roStrain, epsilon, 1e-9 * Math.max(1.0, epsilon),
                    "σn=" + sn + " 不满足 RO 本构");

            // Neuber：σ·ε = (Kt·σn)²/E（弹性点严格恒等）
            double lhs = s * epsilon;
            double rhs = (3.0 * sn) * (3.0 * sn) / 206000.0;
            assertEquals(rhs, lhs, 1e-7 * Math.max(1.0, rhs),
                    "σn=" + sn + " 不满足 Neuber 恒等式");
        }
    }

    @Test
    void higherKtIsMoreSevereAtSameNominalStress() {
        double nominal = 200.0;
        NotchResult kt1 = solver.solve(STEEL, 1.0, nominal);
        NotchResult kt2 = solver.solve(STEEL, 2.0, nominal);
        NotchResult kt3 = solver.solve(STEEL, 3.0, nominal);

        assertTrue(kt2.localStress() > kt1.localStress(), "Kt 变大，缺口更苛刻，应力应更高");
        assertTrue(kt3.localStress() > kt2.localStress());
        assertTrue(kt2.localStrain() > kt1.localStrain(), "Kt 变大，应变也应更高");
        assertTrue(kt3.localStrain() > kt2.localStrain());
        assertTrue(kt3.plasticStrain() > kt2.plasticStrain());
    }

    @Test
    void plasticStressGrowthSlowsWhileStrainAccelerates() {
        // 沿加载序列：名义应力翻倍时，塑性区真实应力涨幅变小，应变涨幅变大
        NotchResult a = solver.solve(STEEL, 3.0, 150.0);
        NotchResult b = solver.solve(STEEL, 3.0, 300.0);
        assertTrue(b.plastic() && a.plastic());

        // 弹性外推应力翻倍
        assertEquals(2.0, b.elasticStress() / a.elasticStress(), 0.0);
        // 真实应力涨幅明显小于翻倍（增长放缓）
        double stressRatio = b.localStress() / a.localStress();
        assertTrue(stressRatio < 1.8, "塑性区真实应力应放缓增长，比值: " + stressRatio);
        // 真实应变涨幅大于真实应力涨幅（应变加快）
        double strainRatio = b.localStrain() / a.localStrain();
        assertTrue(strainRatio > stressRatio,
                "应变增长应快于应力增长: strainRatio=" + strainRatio + ", stressRatio=" + stressRatio);
    }

    @Test
    void convergenceFailureIsReportedAsErrorNotSuspiciousNumber() {
        // 用迭代上限极小的求根器强制不收敛；σn=200 确在塑性区必须走求根
        NeuberSolver doomed = new NeuberSolver(new BisectionRootFinder(1));
        NonConvergenceException ex = assertThrows(NonConvergenceException.class,
                () -> doomed.solve(STEEL, 3.0, 200.0));
        assertTrue(ex.getMessage().contains("不收敛"));
    }
}
