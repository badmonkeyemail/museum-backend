package com.hml.museum.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应体切面
 * 当 Controller 方法返回 null 或没有返回值（即原本使用 @ResponseStatus(HttpStatus.NO_CONTENT) 的场景）时，
 * 自动将其包装为自定义的 Result.noContent()（即 {"code": 20400, "message": "暂无相关内容", "data": null}）。
 */

@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // 1. 当 Controller 方法返回 null (原 204 场景) 时，自动封装为自定义的 noContent
        if (body == null) {
            return Result.noContent();
        }

        // 2. 如果已经是 Result 格式 (如在 Controller 里手动返回了 Result，或者经过了 GlobalExceptionHandler)，直接返回
        if (body instanceof Result) {
            return body;
        }

        // 3. 处理 String 类型的特殊情况，防止底层转换异常
        if (body instanceof String) {
            try {
                return objectMapper.writeValueAsString(Result.success(body));
            } catch (JsonProcessingException e) {
                return Result.fail(ResultCode.SYSTEM_ERROR);
            }
        }

        // 4. 其他普通对象自动包裹为 Result.success(data)
        return Result.success(body);
    }
}