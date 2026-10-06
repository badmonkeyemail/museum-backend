package com.hml.museum.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String message;
    private T data;

    // 私有化构造方法，通过静态方法创建对象
    private Result() {}

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ======================== 成功响应 ========================

    // 默认成功带数据
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    // 默认成功无数据
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    // 自定义成功响应（搭配 ResultCode 中的 SUCCESS_INSERT / SUCCESS_UPDATE / SUCCESS_DELETE）
    public static <T> Result<T> success(ResultCode resultCode, T data) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), data);
    }

    public static <T> Result<T> success(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    // ======================== 无内容/空数据响应 (替换 204) ========================

    public static <T> Result<T> noContent() {
        return new Result<>(ResultCode.NO_CONTENT.getCode(), ResultCode.NO_CONTENT.getMessage(), null);
    }

    public static <T> Result<T> noContent(String customMessage) {
        return new Result<>(ResultCode.NO_CONTENT.getCode(), customMessage, null);
    }

    // ======================== 失败响应 ========================

    // 支持通过 ResultCode 枚举返回
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    // 支持自定义 code 和 message
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    // 默认系统失败
    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCode.SYSTEM_ERROR.getCode(), message, null);
    }
}