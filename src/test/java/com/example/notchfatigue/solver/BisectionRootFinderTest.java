package com.example.notchfatigue.solver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自研求根器测试，含不收敛报错路径。
 */
class BisectionRootFinderTest {

    private final BisectionRootFinder finder = new BisectionRootFinder();

    @Test
    void findsRootOfQuadratic() {
        // x² − 4 = 0 在 [0, 5] 的根为 2
        double root = finder.findRoot(x -> x * x - 4.0, 0.0, 5.0);
        assertEquals(2.0, root, 1e-9);
    }

    @Test
    void findsRootAtLowerZero() {
        assertEquals(0.0, finder.findRoot(x -> x * (x - 3.0), 0.0, 2.0), 0.0);
    }

    @Test
    void findsRootAtUpperZero() {
        assertEquals(3.0, finder.findRoot(x -> x * (x - 3.0), 1.0, 3.0), 0.0);
    }

    @Test
    void findsRootOfPowerLaw() {
        // x^3.7 − 10 = 0
        double target = Math.pow(10.0, 1.0 / 3.7);
        double root = finder.findRoot(x -> Math.pow(x, 3.7) - 10.0, 0.0, 10.0);
        assertEquals(target, root, 1e-8);
    }

    @Test
    void rejectsBracketWithoutSignChange() {
        // f(1)=1, f(2)=4 同号，未夹根
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> finder.findRoot(x -> x * x, 1.0, 2.0));
        assertTrue(ex.getMessage().contains("夹住根"));
    }

    @Test
    void rejectsInvalidBracket() {
        assertThrows(IllegalArgumentException.class,
                () -> finder.findRoot(x -> x, 2.0, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> finder.findRoot(x -> x, Double.NaN, 1.0));
    }

    @Test
    void throwsNonConvergenceWhenIterationCapTooSmall() {
        // 最大 1 次迭代不可能把 [0, 100] 收到 1e-12 容差
        BisectionRootFinder capped = new BisectionRootFinder(1);
        NonConvergenceException ex = assertThrows(NonConvergenceException.class,
                () -> capped.findRoot(x -> x * x - 2.0, 0.0, 100.0));
        assertTrue(ex.getMessage().contains("不收敛"));
    }

    @Test
    void throwsNonConvergenceOnNonFiniteValues() {
        // 端点 f(0)=-10、f(100)=90 夹着根 x=10，但区间内部 [10, 90] 全是 NaN。
        // Newton 步与对分兜底一旦踏入该区域，必须按错误抛出，不能返回可疑数值。
        BisectionRootFinder capped = new BisectionRootFinder(200);
        NonConvergenceException ex = assertThrows(NonConvergenceException.class, () ->
                capped.findRoot(x -> x < 10.0 ? x - 10.0 : (x > 90.0 ? x - 10.0 : Double.NaN),
                        0.0, 100.0));
        assertTrue(ex.getMessage().contains("非有限") || ex.getMessage().contains("不收敛"));
    }
}
