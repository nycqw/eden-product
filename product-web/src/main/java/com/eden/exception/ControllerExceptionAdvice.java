package com.eden.exception;

import com.eden.domain.result.Result;
import com.eden.enums.ResultEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author chenqw
 * @date 2018/11/5
 */
@ControllerAdvice
@Slf4j
public class ControllerExceptionAdvice {

    /**
     * 未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result handleException(Exception e) {
        log.error(ResultEnum.UNKNOWN_ERROR.getMessage(), e);
        return Result.fail(ResultEnum.UNKNOWN_ERROR.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Result handleArgumentException(MethodArgumentNotValidException e) {
        FieldError error = (FieldError) e.getBindingResult().getAllErrors().get(0);
        String message = "【" + error.getField() + "】" + error.getDefaultMessage();
        return Result.fail(ResultEnum.PARAM_ERROR.getCode(), message);
    }
}
