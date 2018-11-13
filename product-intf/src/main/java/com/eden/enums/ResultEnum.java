package com.eden.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ResultEnum {

    SUCCESS(1, "成功"),
    SYSTEM_ERROR(-1, "系统异常"),
    TRADE_ERROR(-2, "业务异常"),
    DATABASE_ERROR(-3, "数据库异常"),
    PARAM_ERROR(-4, "参数异常"),
    UNKNOWN_ERROR(-5, "未知异常");

    @Getter
    private int code;
    @Getter
    private String message;
}
