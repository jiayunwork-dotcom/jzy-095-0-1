package com.example.notchfatigue.model;

/**
 * Ramberg–Osgood 单调本构材料参数。
 *
 * <p>总应变 ε = σ/E + (σ/K)^(1/n)
 *
 * @param e  弹性模量 E（应力单位，如 MPa）
 * @param k  强度系数 K（应力单位，如 MPa）
 * @param n  硬化指数 n（无量纲，0 &lt; n &lt; 1）
 */
public record MaterialProperties(double e, double k, double n) {
}
