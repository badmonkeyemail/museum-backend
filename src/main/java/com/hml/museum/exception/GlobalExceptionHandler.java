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
    public Result<Void> handleBusinessException(BusinessException e) {
        String location = getExceptionLocation(e);
        log.warn("业务逻辑异常 | 类型={}, 位置={}, 错误码={}, 信息={}",
                e.getClass().getSimpleName(), location, e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理非法状态异常（保留您原本的逻辑）
     */
    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalStateException(IllegalStateException e) {
        String location = getExceptionLocation(e);
        log.error("状态异常 | 类型={}, 位置={}, 信息={}",
                e.getClass().getSimpleName(), location, e.getMessage(), e);
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
        String location = getExceptionLocation(e);
        log.warn("请求参数校验未通过 | 类型={}, 位置={}, 错误详情={}",
                e.getClass().getSimpleName(), location, errors);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "请求参数格式错误");
    }

    /**
     * 处理所有其他未被精确捕获的异常（兜底处理，保留您原本的逻辑并适配 ResultCode）
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("发生未知系统异常 | 类型={}, 位置={}, 信息={}",
                e.getClass().getName(), getExceptionLocation(e), e.getMessage(), e);
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }

    /**
     * 从异常堆栈中提取真正抛出异常的位置（类 + 方法 + 行号）
     * 优先取第一个非框架、非当前Handler的堆栈帧
     */
    private String getExceptionLocation(Throwable e) {
        if (e == null || e.getStackTrace() == null || e.getStackTrace().length == 0) {
            return "unknown";
        }

        for (StackTraceElement element : e.getStackTrace()) {
            String className = element.getClassName();
            // 跳过常见框架和当前全局异常处理器本身，尽量定位到业务代码
            if (className.startsWith("org.springframework")
                    || className.startsWith("org.apache")
                    || className.startsWith("java.")
                    || className.startsWith("javax.")
                    || className.startsWith("jakarta.")
                    || className.startsWith("sun.")
                    || className.startsWith("com.sun.")
                    || className.contains("GlobalExceptionHandler")
                    || className.contains("CGLIB")
                    || className.contains("$$")) {
                continue;
            }
            return String.format("%s.%s(%s:%d)",
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName(),
                    element.getLineNumber());
        }

        // 如果过滤后没有找到，就用堆栈第一个
        StackTraceElement first = e.getStackTrace()[0];
        return String.format("%s.%s(%s:%d)",
                first.getClassName(),
                first.getMethodName(),
                first.getFileName(),
                first.getLineNumber());
    }

}