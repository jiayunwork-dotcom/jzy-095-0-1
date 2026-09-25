package com.example.notchfatigue.constitutive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ramberg–Osgood 本构测试。
 * 重点盯住塑性项幂次必须是 1/n（而非 n）。
 */
class RambergOsgoodTest {

    private final RambergOsgood ro = new RambergOsgood(206000.0, 1200.0, 0.2);

    @Test
    void elasticStrainIsStressOverE() {
        assertEquals(300.0 / 206000.0, ro.elasticStrain(300.0), 1e-15);
    }

    @Test
    void zeroStressGivesZeroStrain() {
        assertEquals(0.0, ro.totalStrain(0.0), 0.0);
        assertEquals(0.0, ro.plasticStrain(0.0), 0.0);
    }

    @Test
    void plasticTermUsesPowerOneOverN() {
        double stress = 600.0;
        // (σ/K)^(1/n) = (600/1200)^5 = 0.5^5 = 0.03125
        assertEquals(0.03125, ro.plasticStrain(stress), 1e-12);
        // 反方向的错误写法 (σ/K)^n = 0.5^0.2 ≈ 0.8706，必须明显不同
        double wrongPowerN = Math.pow(0.5, 0.2);
        assertTrue(Math.abs(ro.plasticStrain(stress) - wrongPowerN) > 0.8,
                "塑性项幂次被误写成 n 时应与正确值显著不同");
    }

    @Test
    void totalStrainIsElasticPlusPlastic() {
        double stress = 600.0;
        double expected = 600.0 / 206000.0 + 0.03125;
        assertEquals(expected, ro.totalStrain(stress), 1e-14);
    }

    @Test
    void lowStressIsEffectivelyElastic() {
        // σ/E = 4.85e-6，塑性项 (1/1200)^5 ≈ 4.0e-16，比例远小于 1e-6
        assertTrue(ro.isEffectivelyElastic(1.0));
        assertTrue(ro.isEffectivelyElastic(0.0));
    }

    @Test
    void highStressIsNotEffectivelyElastic() {
        // σ=1200 时塑性项 = 1，远大于弹性项
        assertTrue(!ro.isEffectivelyElastic(1200.0));
    }

    @Test
    void hysteresisRangeUsesTwoTimesScaling() {
        double ds = 1200.0;
        // Δε = 1200/206000 + 2·(1200/2400)^5
        double expected = 1200.0 / 206000.0 + 2.0 * Math.pow(0.5, 5);
        assertEquals(expected, ro.hysteresisStrainRange(ds), 1e-14);
    }

    @Test
    void rejectsNonPositiveParameters() {
        assertThrows(IllegalArgumentException.class, () -> new RambergOsgood(0.0, 1200.0, 0.2));
        assertThrows(IllegalArgumentException.class, () -> new RambergOsgood(-206000.0, 1200.0, 0.2));
        assertThrows(IllegalArgumentException.class, () -> new RambergOsgood(206000.0, 0.0, 0.2));
        assertThrows(IllegalArgumentException.class, () -> new RambergOsgood(206000.0, -1200.0, 0.2));
        assertThrows(IllegalArgumentException.class, () -> new RambergOsgood(206000.0, 1200.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new RambergOsgood(206000.0, 1200.0, -0.2));
        assertThrows(IllegalArgumentException.class, () -> new RambergOsgood(Double.NaN, 1200.0, 0.2));
    }
}
