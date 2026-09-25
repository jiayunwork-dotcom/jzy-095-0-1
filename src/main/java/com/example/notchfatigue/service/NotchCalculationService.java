package com.example.notchfatigue.service;

import com.example.notchfatigue.model.NotchRequest;
import com.example.notchfatigue.model.NotchResult;
import com.example.notchfatigue.model.SequenceRequest;
import com.example.notchfatigue.model.SequenceResult;
import com.example.notchfatigue.model.UnloadRequest;
import com.example.notchfatigue.model.UnloadResult;
import com.example.notchfatigue.solver.NeuberSolver;
import com.example.notchfatigue.solver.NeuberUnloader;
import com.example.notchfatigue.solver.NonConvergenceException;
import com.example.notchfatigue.validation.InputValidator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 核算编排服务：校验 → 单点/批量求根。
 *
 * <p>批量序列里每个名义应力都独立调用一次 {@link NeuberSolver}，
 * 点与点之间不共享任何中间状态；某一点求根失败只报告该点，
 * 绝不返回可疑数值替代。
 */
@Service
public class NotchCalculationService {

    private final InputValidator validator;
    private final NeuberSolver neuberSolver;
    private final NeuberUnloader unloader;

    public NotchCalculationService(InputValidator validator,
                                   NeuberSolver neuberSolver,
                                   NeuberUnloader unloader) {
        this.validator = validator;
        this.neuberSolver = neuberSolver;
        this.unloader = unloader;
    }

    public NotchResult solve(NotchRequest request) {
        validator.validate(request);
        return neuberSolver.solve(request.material(), request.kt(), request.nominalStress());
    }

    public SequenceResult solveSequence(SequenceRequest request) {
        validator.validate(request);
        List<NotchResult> points = new ArrayList<>(request.nominalStresses().size());
        for (Double nominal : request.nominalStresses()) {
            try {
                points.add(neuberSolver.solve(request.material(), request.kt(), nominal));
            } catch (NonConvergenceException e) {
                throw new NonConvergenceException(
                        "名义应力 " + nominal + " 处求根不收敛：" + e.getMessage());
            }
        }
        return new SequenceResult(request.kt(), request.material(), points);
    }

    public UnloadResult unload(UnloadRequest request) {
        validator.validate(request);
        return unloader.unload(request.material(), request.kt(),
                request.peakNominalStress(), request.targetNominalStress());
    }
}
