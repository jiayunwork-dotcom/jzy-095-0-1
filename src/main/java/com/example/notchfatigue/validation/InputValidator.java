package com.example.notchfatigue.validation;

import com.example.notchfatigue.model.MaterialProperties;
import com.example.notchfatigue.model.NotchRequest;
import com.example.notchfatigue.model.SequenceRequest;
import com.example.notchfatigue.model.UnloadRequest;

import java.util.List;

/**
 * 输入校验，独立成类：所有非法输入都以“带原因”的 {@link IllegalArgumentException} 返回。
 *
 * <p>规则（来自需求）：
 * <ul>
 *     <li>弹性模量 E、强度系数 K、硬化指数 n 必须为正（非正即拒）；</li>
 *     <li>应力集中系数 Kt 必须 ≥ 1（小于一即拒）；</li>
 *     <li>名义应力为非负有限值；</li>
 *     <li>非有限值（NaN/Infinity）一律拒绝。</li>
 * </ul>
 */
public final class InputValidator {

    /** 防止极端参数让 σ²/E 在乘法中溢出，给一个宽裕的工程上界。 */
    private static final double MAX_VALUE = 1.0e12;

    public void validate(NotchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        validateMaterial(request.material());
        validateKt(request.kt());
        validateNominalStress(request.nominalStress(), "名义应力");
    }

    public void validate(SequenceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        validateMaterial(request.material());
        validateKt(request.kt());
        List<Double> stresses = request.nominalStresses();
        if (stresses == null || stresses.isEmpty()) {
            throw new IllegalArgumentException("名义应力序列不能为空");
        }
        for (int i = 0; i < stresses.size(); i++) {
            Double s = stresses.get(i);
            if (s == null) {
                throw new IllegalArgumentException("名义应力序列第 " + (i + 1) + " 个元素为空");
            }
            validateNominalStress(s, "名义应力序列第 " + (i + 1) + " 个元素");
        }
    }

    public void validate(UnloadRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        validateMaterial(request.material());
        validateKt(request.kt());
        validateNominalStress(request.peakNominalStress(), "峰值名义应力");
        validateNominalStress(request.targetNominalStress(), "卸载目标名义应力");
        if (request.targetNominalStress() > request.peakNominalStress()) {
            throw new IllegalArgumentException(
                    "卸载目标名义应力 (" + request.targetNominalStress()
                            + ") 不能大于峰值名义应力 (" + request.peakNominalStress() + ")");
        }
    }

    private void validateMaterial(MaterialProperties material) {
        if (material == null) {
            throw new IllegalArgumentException("材料参数 material 缺失");
        }
        if (!Double.isFinite(material.e()) || material.e() <= 0.0) {
            throw new IllegalArgumentException(
                    "弹性模量 E 必须为正的有限值，收到: " + material.e());
        }
        if (!Double.isFinite(material.k()) || material.k() <= 0.0) {
            throw new IllegalArgumentException(
                    "强度系数 K 必须为正的有限值，收到: " + material.k());
        }
        if (!Double.isFinite(material.n()) || material.n() <= 0.0) {
            throw new IllegalArgumentException(
                    "硬化指数 n 必须为正的有限值，收到: " + material.n());
        }
        if (material.n() >= 1.0) {
            throw new IllegalArgumentException(
                    "硬化指数 n 通常应小于 1，收到: " + material.n());
        }
        if (material.e() > MAX_VALUE || material.k() > MAX_VALUE) {
            throw new IllegalArgumentException(
                    "材料参数超出可计算上界 " + MAX_VALUE + "（防止数值溢出）");
        }
    }

    private void validateKt(double kt) {
        if (!Double.isFinite(kt)) {
            throw new IllegalArgumentException("应力集中系数 Kt 必须为有限值，收到: " + kt);
        }
        if (kt < 1.0) {
            throw new IllegalArgumentException(
                    "应力集中系数 Kt 不能小于 1（Kt=1 表示无缺口），收到: " + kt);
        }
        if (kt > MAX_VALUE) {
            throw new IllegalArgumentException("应力集中系数 Kt 超出可计算上界 " + MAX_VALUE);
        }
    }

    private void validateNominalStress(double stress, String label) {
        if (!Double.isFinite(stress)) {
            throw new IllegalArgumentException(label + " 必须为有限值，收到: " + stress);
        }
        if (stress < 0.0) {
            throw new IllegalArgumentException(label + " 不能为负（单调加载核算取非负值），收到: " + stress);
        }
        if (stress > MAX_VALUE) {
            throw new IllegalArgumentException(label + " 超出可计算上界 " + MAX_VALUE);
        }
    }
}
