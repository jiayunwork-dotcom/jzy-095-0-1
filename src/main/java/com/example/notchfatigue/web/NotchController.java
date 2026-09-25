package com.example.notchfatigue.web;

import com.example.notchfatigue.model.NotchRequest;
import com.example.notchfatigue.model.NotchResult;
import com.example.notchfatigue.model.SequenceRequest;
import com.example.notchfatigue.model.SequenceResult;
import com.example.notchfatigue.model.UnloadRequest;
import com.example.notchfatigue.model.UnloadResult;
import com.example.notchfatigue.service.BuiltInExample;
import com.example.notchfatigue.service.NotchCalculationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP 接口层（无网页，只有 JSON）：
 *
 * <ul>
 *     <li>POST /api/notch/solve     单点名义应力核算</li>
 *     <li>POST /api/notch/sequence  递增名义应力序列批量核算</li>
 *     <li>POST /api/notch/unload    卸载迟滞薄壳层（Masing）</li>
 *     <li>GET  /api/notch/example   内置 Kt=3 圆角缺口算例</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/notch")
public class NotchController {

    private final NotchCalculationService service;
    private final BuiltInExample builtInExample;

    public NotchController(NotchCalculationService service, BuiltInExample builtInExample) {
        this.service = service;
        this.builtInExample = builtInExample;
    }

    @PostMapping("/solve")
    public NotchResult solve(@RequestBody NotchRequest request) {
        return service.solve(request);
    }

    @PostMapping("/sequence")
    public SequenceResult sequence(@RequestBody SequenceRequest request) {
        return service.solveSequence(request);
    }

    @PostMapping("/unload")
    public UnloadResult unload(@RequestBody UnloadRequest request) {
        return service.unload(request);
    }

    @GetMapping("/example")
    public SequenceResult example() {
        return builtInExample.run();
    }
}
