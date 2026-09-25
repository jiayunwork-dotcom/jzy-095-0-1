package com.example.notchfatigue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 缺口根部弹塑性应力应变核算服务入口。
 *
 * <p>仅通过 HTTP 工作：
 * <ul>
 *     <li>POST /api/notch/solve  —— 单点名义应力核算</li>
 *     <li>POST /api/notch/sequence —— 递增名义应力序列批量核算</li>
 *     <li>POST /api/notch/unload —— 很薄一层卸载迟滞估计（Masing）</li>
 *     <li>GET  /api/notch/example —— 内置 Kt=3 圆角缺口算例</li>
 * </ul>
 */
@SpringBootApplication
public class NotchFatigueApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotchFatigueApplication.class, args);
    }
}
