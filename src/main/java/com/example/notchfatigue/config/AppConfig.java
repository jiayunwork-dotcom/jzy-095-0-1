package com.example.notchfatigue.config;

import com.example.notchfatigue.service.NotchCalculationService;
import com.example.notchfatigue.solver.BisectionRootFinder;
import com.example.notchfatigue.solver.NeuberSolver;
import com.example.notchfatigue.solver.NeuberUnloader;
import com.example.notchfatigue.validation.InputValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 手动装配无框架注解的纯数值组件（求根器、求解器、校验器），
 * 保持本构/求根/校验模块不依赖 Spring。
 */
@Configuration
public class AppConfig {

    @Bean
    public BisectionRootFinder rootFinder() {
        return new BisectionRootFinder();
    }

    @Bean
    public NeuberSolver neuberSolver(BisectionRootFinder rootFinder) {
        return new NeuberSolver(rootFinder);
    }

    @Bean
    public NeuberUnloader neuberUnloader(BisectionRootFinder rootFinder,
                                         NeuberSolver neuberSolver) {
        return new NeuberUnloader(rootFinder, neuberSolver);
    }

    @Bean
    public InputValidator inputValidator() {
        return new InputValidator();
    }

    @Bean
    public NotchCalculationService notchCalculationService(InputValidator validator,
                                                           NeuberSolver neuberSolver,
                                                           NeuberUnloader neuberUnloader) {
        return new NotchCalculationService(validator, neuberSolver, neuberUnloader);
    }
}
