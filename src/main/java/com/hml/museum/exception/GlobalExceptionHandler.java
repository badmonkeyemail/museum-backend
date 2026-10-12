package com.hml.museum.exception;

import com.hml.museum.result.Result;
import com.hml.museum.result.ResultCode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务逻辑异常 (BusinessException)
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e,
                                                HandlerMethod handlerMethod) {
        //log.warn("业务逻辑异常：[{}] {}", e.getCode(), e.getMessage());
        logWithMethod("业务逻辑异常", e, handlerMethod, "code: [{}], msg: {}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理非法状态异常（保留您原本的逻辑）
     */
    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalStateException(IllegalStateException e,
                                                    HandlerMethod handlerMethod) {
        //log.error("状态异常：{}", e.getMessage(), e);
        logWithMethod("状态异常", e, handlerMethod, "msg: {}", e.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 处理 JSR303 参数校验异常 (@Valid / @Validated 失败)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Map<String, String>> handleValidationException(
                                MethodArgumentNotValidException e,
                                HandlerMethod handlerMethod) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        //log.warn("请求参数校验未通过：{}", errors);
        logWithMethod("请求参数校验未通过", e, handlerMethod, "错误详情: {}", errors);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "请求参数格式错误");
    }

    /**
     * 处理所有其他未被精确捕获的异常（兜底处理，保留您原本的逻辑并适配 ResultCode）
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HandlerMethod handlerMethod) {
        //log.error("发生未知系统异常：", e);
        logWithMethod("未知系统异常", e, handlerMethod, "msg: {}", e.getMessage());
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }

    /**
     * 统一提取类名和方法名的公共日志方法
     */
    private void logWithMethod(String title, Exception e, HandlerMethod handlerMethod, String messagePattern, Object... args) {
        if (handlerMethod != null) {
            String className = handlerMethod.getBeanType().getName();
            String methodName = handlerMethod.getMethod().getName();
            log.error("{} -> 类: [{}], 方法: [{}], " + messagePattern,
                    Stream.concat(Stream.of(className, methodName), Arrays.stream(args)).toArray());
        } else {
            log.error("{} -> " + messagePattern, args);
        }
    }

}