package com.example.notchfatigue.web;

import com.example.notchfatigue.solver.NonConvergenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 错误映射：输入非法 → 400；求根不收敛 → 422；请求体无法解析 → 400。
 * 所有错误都带原因返回，不吞异常。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException e) {
        return new ErrorResponse("INVALID_INPUT", e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadable(HttpMessageNotReadableException e) {
        return new ErrorResponse("INVALID_INPUT", "请求体不是合法 JSON 或字段缺失");
    }

    @ExceptionHandler(NonConvergenceException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleNonConvergence(NonConvergenceException e) {
        return new ErrorResponse("NON_CONVERGENCE", e.getMessage());
    }
}
