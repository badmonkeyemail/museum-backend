package com.hml.museum.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    // 成功
    SUCCESS(200, "操作成功"),
    SUCCESS_INSERT(20100, "添加成功"),
    SUCCESS_UPDATE(20200, "修改成功"),
    SUCCESS_DELETE(20300, "删除成功"),
    NO_CONTENT(20400, "无内容"),

    // 客户端错误 (4xxx)
    PARAM_ERROR(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "未经授权或 Token 已过期"),
    FORBIDDEN(40300, "权限不足，拒绝访问"),
    NOT_FOUND(40400, "请求的资源不存在"),

    // 业务逻辑错误 (5xxx)
    USER_NOT_EXIST(50001, "用户不存在"),
    USER_ALREADY_EXISTS(50002, "用户已存在"),
    ACCOUNT_LOCKED(50003, "账号已被冻结"),

    // 系统错误
    SYSTEM_ERROR(50000, "系统繁忙，请稍后再试");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}