package com.example.notchfatigue.service;

import com.example.notchfatigue.model.SequenceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 服务启动后载入内置 Kt=3 算例，把弹塑性结果与弹性对照一并打出到日志。
 */
@Component
public class ExampleRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExampleRunner.class);

    private final BuiltInExample builtInExample;

    public ExampleRunner(BuiltInExample builtInExample) {
        this.builtInExample = builtInExample;
    }

    @Override
    public void run(ApplicationArguments args) {
        SequenceResult result = builtInExample.run();
        log.info(BuiltInExample.renderTable(result));
    }
}
