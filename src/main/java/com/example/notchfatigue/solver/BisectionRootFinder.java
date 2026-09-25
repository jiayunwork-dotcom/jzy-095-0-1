package com.example.notchfatigue.solver;

import java.util.function.DoubleUnaryOperator;

/**
 * 自研数值求根器（不依赖任何外部数学库）。
 *
 * <p>方法：对分法（bisection）。Neuber 联立残差在 σ ≥ 0 上严格单调、
 * 且已知根被夹在 [0, Kt·σn] 内，对分法对这类问题每迭代一次确定地把区间减半，
 * 不会发散、不依赖导数，约 60 次迭代即可达到 1e-12 相对容差。
 *
 * <p>唯一的失败方式是达到迭代上限仍未满足容差——此时抛
 * {@link NonConvergenceException}，调用方必须按错误处理，绝不返回可疑数值。
 */
public final class BisectionRootFinder {

    private static final int DEFAULT_MAX_ITERATIONS = 200;
    private static final double REL_TOLERANCE = 1.0e-12;
    private static final double ABS_TOLERANCE = 1.0e-12;

    private final int maxIterations;

    public BisectionRootFinder() {
        this(DEFAULT_MAX_ITERATIONS);
    }

    public BisectionRootFinder(int maxIterations) {
        if (maxIterations < 1) {
            throw new IllegalArgumentException("最大迭代次数必须为正: " + maxIterations);
        }
        this.maxIterations = maxIterations;
    }

    /**
     * 在 [low, high] 内求 f(x) = 0 的根，要求 f(low) ≤ 0 ≤ f(high)（端点为 0 直接返回）。
     *
     * @throws NonConvergenceException 超过迭代上限仍未达到容差，或途中出现非有限函数值
     * @throws IllegalArgumentException 初始括号不夹根，或端点函数值非有限
     */
    public double findRoot(DoubleUnaryOperator f, double low, double high) {
        if (!(low < high) || !Double.isFinite(low) || !Double.isFinite(high)) {
            throw new IllegalArgumentException(
                    "求根括号非法，要求 low < high 且均有限: low=" + low + ", high=" + high);
        }

        double fLow = f.applyAsDouble(low);
        double fHigh = f.applyAsDouble(high);
        if (!Double.isFinite(fLow) || !Double.isFinite(fHigh)) {
            throw new IllegalArgumentException(
                    "求根端点函数值非有限: f(low)=" + fLow + ", f(high)=" + fHigh);
        }
        if (fLow == 0.0) {
            return low;
        }
        if (fHigh == 0.0) {
            return high;
        }
        if (fLow > 0.0 || fHigh < 0.0) {
            throw new IllegalArgumentException(String.format(
                    "求根括号未夹住根（要求 f(low) <= 0 <= f(high)）: f(%g)=%g, f(%g)=%g",
                    low, fLow, high, fHigh));
        }

        for (int i = 0; i < maxIterations; i++) {
            double mid = (low + high) / 2.0;
            double fMid = f.applyAsDouble(mid);
            if (!Double.isFinite(fMid)) {
                throw new NonConvergenceException(String.format(
                        "求根过程中函数值非有限，x=%g，已迭代 %d 次", mid, i));
            }
            if (fMid == 0.0) {
                return mid;
            }
            if (fMid < 0.0) {
                low = mid;
                fLow = fMid;
            } else {
                high = mid;
                fHigh = fMid;
            }

            double span = high - low;
            if (span <= REL_TOLERANCE * Math.max(1.0, high) + ABS_TOLERANCE) {
                return (low + high) / 2.0;
            }
        }

        throw new NonConvergenceException(String.format(
                "求根不收敛：%d 次迭代后剩余括号仍为 [%g, %g]，f(low)=%g, f(high)=%g",
                maxIterations, low, high, fLow, fHigh));
    }
}
