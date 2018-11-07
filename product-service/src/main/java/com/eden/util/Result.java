package com.eden.util;

public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    private Result(Integer code, String message, T data){
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static final Integer SUCCESS = 0;
    public static final Integer SYSTEM_ERROR = -1;
    public static final Integer TRADE_ERROR = -2;
    public static final Integer DATABASE_ERROR = -3;
    public static final Integer PARAM_ERROR = -4;

    public boolean isSuccess(){
        return SUCCESS.equals(code);
    }

    public static <T> Result<T> success(){
        return success("成功", null);
    }

    public static <T> Result<T> success(T data){
        return success("成功", data);
    }

    public static <T> Result<T> success(String message, T data){
        return new Result(SUCCESS, message, data);
    }

    public static <T> Result<T> fail(Integer code, String message){
        return fail(code, message, null);
    }

    public static <T> Result<T> fail(Integer code, String message, T data){
        return new Result(code, message, data);
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

}
