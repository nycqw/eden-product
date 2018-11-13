package com.eden.domain.result;

import com.eden.enums.ResultEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Result<T> {

    @Getter
    private Integer code;
    @Getter
    private String message;
    @Getter
    private T data;

    public static <T> Result<T> success(){
        return success("成功", null);
    }

    public static <T> Result<T> success(T data){
        return success("成功", data);
    }

    public static <T> Result<T> success(String message, T data){
        return new Result(ResultEnum.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> fail(Integer code, String message){
        return fail(code, message, null);
    }

    public static <T> Result<T> fail(Integer code, String message, T data){
        return new Result(code, message, data);
    }

}
