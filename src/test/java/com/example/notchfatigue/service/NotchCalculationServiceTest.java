package com.example.notchfatigue.service;

import com.example.notchfatigue.model.MaterialProperties;
import com.example.notchfatigue.model.NotchRequest;
import com.example.notchfatigue.model.NotchResult;
import com.example.notchfatigue.model.SequenceRequest;
import com.example.notchfatigue.model.SequenceResult;
import com.example.notchfatigue.model.UnloadRequest;
import com.example.notchfatigue.model.UnloadResult;
import com.example.notchfatigue.solver.BisectionRootFinder;
import com.example.notchfatigue.solver.NeuberSolver;
import com.example.notchfatigue.solver.NeuberUnloader;
import com.example.notchfatigue.solver.NonConvergenceException;
import com.example.notchfatigue.validation.InputValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 服务编排、批量序列（各点独立）与卸载薄壳层测试。
 */
class NotchCalculationServiceTest {

    private static final MaterialProperties STEEL =
            new MaterialProperties(206000.0, 1200.0, 0.2);

    private NotchCalculationService service;

    @BeforeEach
    void setUp() {
        BisectionRootFinder finder = new BisectionRootFinder();
        NeuberSolver solver = new NeuberSolver(finder);
        service = new NotchCalculationService(new InputValidator(), solver,
                new NeuberUnloader(finder, solver));
    }

    @Test
    void sequenceCoversElasticToPlasticAndPointsAreIndependent() {
        List<Double> nominal = List.of(1.0, 50.0, 100.0, 200.0, 400.0);
        SequenceResult result = service.solveSequence(new SequenceRequest(STEEL, 3.0, nominal));

        assertEquals(5, result.points().size());
        assertEquals(3.0, result.kt());

        // 首点弹性，严格 σ=Kt·σn
        NotchResult first = result.points().get(0);
        assertFalse(first.plastic());
        assertEquals(3.0, first.localStress(), 1e-12);

        // 末点深度塑性
        NotchResult last = result.points().get(4);
        assertTrue(last.plastic());
        assertTrue(last.localStress() < 1200.0);

        // 单调不减
        for (int i = 1; i < result.points().size(); i++) {
            assertTrue(result.points().get(i).localStress()
                    >= result.points().get(i - 1).localStress());
            assertTrue(result.points().get(i).localStrain()
                    >= result.points().get(i - 1).localStrain());
        }

        // 各点独立：乱序重算首点，结果必须与序列中的首点完全一致
        NotchResult recomputed = service.solve(new NotchRequest(STEEL, 3.0, 1.0));
        assertEquals(first.localStress(), recomputed.localStress(), 0.0);
        assertEquals(first.localStrain(), recomputed.localStrain(), 0.0);
    }

    @Test
    void nonMonotonicSequenceIsAlsoSolvedIndependently() {
        // 序列不要求递增：每个点各自求根，先大后小也不应受前一点污染
        SequenceResult result = service.solveSequence(
                new SequenceRequest(STEEL, 3.0, List.of(300.0, 100.0)));
        NotchResult big = result.points().get(0);
        NotchResult small = result.points().get(1);
        assertTrue(big.localStress() > small.localStress());
        assertEquals(300.0 * 3.0, service.solve(new NotchRequest(STEEL, 3.0, 300.0)).elasticStress(), 0.0);
    }

    @Test
    void sequenceFailureMentionsFailingNominalStress() {
        // 全部参数合法，但注入迭代上限极小的求解器，使塑性点失败
        BisectionRootFinder capped = new BisectionRootFinder(1);
        NeuberSolver doomedSolver = new NeuberSolver(capped);
        NotchCalculationService doomed = new NotchCalculationService(
                new InputValidator(), doomedSolver, new NeuberUnloader(capped, doomedSolver));

        NonConvergenceException ex = assertThrows(NonConvergenceException.class,
                () -> doomed.solveSequence(new SequenceRequest(STEEL, 3.0, List.of(1.0, 200.0))));
        assertTrue(ex.getMessage().contains("200.0"));
    }

    @Test
    void unloadFromPeakFollowsMasingBranch() {
        UnloadResult r = service.unload(new UnloadRequest(STEEL, 3.0, 200.0, 100.0));

        // 峰值单调解与直接求解一致
        NotchResult peak = service.solve(new NotchRequest(STEEL, 3.0, 200.0));
        assertEquals(peak.localStress(), r.peakLocalStress(), 1e-9);
        assertEquals(peak.localStrain(), r.peakLocalStrain(), 1e-12);

        // 卸载变程为正，目标点低于峰值点
        assertTrue(r.deltaStress() > 0.0);
        assertTrue(r.deltaStrain() > 0.0);
        assertEquals(r.peakLocalStress() - r.targetLocalStress(), r.deltaStress(), 1e-9);
        assertEquals(r.peakLocalStrain() - r.targetLocalStrain(), r.deltaStrain(), 1e-9);

        // Masing 分支本构自洽：Δε = Δσ/E + 2·(Δσ/(2K))^(1/n)
        double ds = r.deltaStress();
        double expectedRange = ds / 206000.0 + 2.0 * Math.pow(ds / 2400.0, 5.0);
        assertEquals(expectedRange, r.deltaStrain(), 1e-9 * expectedRange);
    }

    @Test
    void unloadToSameStressGivesZeroRanges() {
        UnloadResult r = service.unload(new UnloadRequest(STEEL, 3.0, 200.0, 200.0));
        assertEquals(0.0, r.deltaStress(), 0.0);
        assertEquals(0.0, r.deltaStrain(), 0.0);
        assertEquals(r.peakLocalStress(), r.targetLocalStress(), 0.0);
    }

    @Test
    void invalidInputFromServiceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.solve(new NotchRequest(
                        new MaterialProperties(-1.0, 1200.0, 0.2), 3.0, 100.0)));
    }
}
