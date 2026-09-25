package com.example.notchfatigue.solver;

/**
 * 求根不收敛时抛出。调用方必须把它作为错误返回，
 * 而不是吞掉异常或返回一个可疑的数。
 */
public class NonConvergenceException extends RuntimeException {

    public NonConvergenceException(String message) {
        super(message);
    }
}
