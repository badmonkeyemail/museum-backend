package com.hml.museum.service;

/**
 * 查询操作符。
 */
public enum SearchOperator {

    /**
     * 包含。
     *
     * SQL：
     * LIKE '%xxx%'
     */
    CONTAINS,

    /**
     * 以指定字符串开头。
     *
     * SQL：
     * LIKE 'xxx%'
     */
    STARTS_WITH,

    /**
     * 以指定字符串结尾。
     *
     * SQL：
     * LIKE '%xxx'
     */
    ENDS_WITH,

    /**
     * 等于。
     *
     * SQL：
     * = ?
     */
    EQUALS,

    /**
     * 时间范围查询。
     *
     * value1、value2 可以只填写一个。
     */
    BETWEEN
}