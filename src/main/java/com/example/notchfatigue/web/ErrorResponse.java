package com.example.notchfatigue.web;

/**
 * 统一错误响应体。
 *
 * @param error   错误类别
 * @param reason  具体原因
 */
public record ErrorResponse(String error, String reason) {
}
