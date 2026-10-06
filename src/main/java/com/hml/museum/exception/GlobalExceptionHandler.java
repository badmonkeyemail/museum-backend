package com.hml.museum.exception;

import com.hml.museum.result.Result;
import com.hml.museum.result.ResultCode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务逻辑异常 (BusinessException)
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务逻辑异常：[{}] {}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理非法状态异常（保留您原本的逻辑）
     */
    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalStateException(IllegalStateException e) {
        log.error("状态异常：{}", e.getMessage(), e);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 处理 JSR303 参数校验异常 (@Valid / @Validated 失败)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("请求参数校验未通过：{}", errors);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "请求参数格式错误");
    }

    /**
     * 处理所有其他未被精确捕获的异常（兜底处理，保留您原本的逻辑并适配 ResultCode）
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("发生未知系统异常：", e);
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }
}