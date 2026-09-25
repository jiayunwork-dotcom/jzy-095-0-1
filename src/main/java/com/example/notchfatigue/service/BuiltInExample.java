package com.example.notchfatigue.service;

import com.example.notchfatigue.model.MaterialProperties;
import com.example.notchfatigue.model.NotchResult;
import com.example.notchfatigue.model.SequenceRequest;
import com.example.notchfatigue.model.SequenceResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内置 Kt=3 圆角缺口算例（典型结构钢参数，单位 MPa）。
 *
 * <p>名义应力取 0~400 MPa 递增序列：低载荷段真实应力严格等于 3·σn；
 * 超过比例极限后真实应力低于 3·σn、真实应变加速放大，
 * 弹塑性结果与纯弹性外推在输出中并列对照。
 */
@Component
public class BuiltInExample {

    public static final double KT = 3.0;

    /** 结构钢代表值：E=206000 MPa，K=1200 MPa，n=0.2。 */
    public static final MaterialProperties MATERIAL = new MaterialProperties(206000.0, 1200.0, 0.2);

    public static final List<Double> NOMINAL_STRESSES = List.of(
              0.0,   1.0,   2.0,   3.0,   5.0,  10.0,  20.0,
             40.0,  60.0,  80.0, 100.0, 120.0, 150.0, 180.0,
            220.0, 260.0, 300.0, 350.0, 400.0);

    private final NotchCalculationService service;

    public BuiltInExample(NotchCalculationService service) {
        this.service = service;
    }

    public SequenceResult run() {
        return service.solveSequence(new SequenceRequest(MATERIAL, KT, NOMINAL_STRESSES));
    }

    /** 生成启动日志用的对照表文本。 */
    public static String renderTable(SequenceResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        sb.append("内置算例: Kt=3 圆角缺口, E=").append(MATERIAL.e())
          .append(" MPa, K=").append(MATERIAL.k())
          .append(" MPa, n=").append(MATERIAL.n())
          .append("  (σ: MPa, ε 无量纲)").append(System.lineSeparator());
        sb.append("  σn      | 弹性 σ=Kt·σn | Neuber真实σ | σ真实/σ弹性 | 弹性 ε    | 真实 ε      | 塑性?")
          .append(System.lineSeparator());
        sb.append("----------+---------------+-------------+-------------+-----------+-------------+------")
          .append(System.lineSeparator());
        for (NotchResult p : result.points()) {
            double ratio = p.elasticStress() == 0.0 ? 1.0
                    : p.localStress() / p.elasticStress();
            sb.append(String.format(
                    " %7.1f | %13.2f | %11.2f | %11.5f | %9.6f | %11.7f | %s%n",
                    p.nominalStress(),
                    p.elasticStress(),
                    p.localStress(),
                    ratio,
                    p.elasticStrain(),
                    p.localStrain(),
                    p.plastic() ? "塑性" : "弹性"));
        }
        return sb.toString();
    }
}
