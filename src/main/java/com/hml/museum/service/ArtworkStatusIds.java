package com.hml.museum.service;

/** 数据库初始化后保持不变的作品状态ID；业务代码不要再直接比较状态字符串。 */
public final class ArtworkStatusIds {
    private ArtworkStatusIds() {}
    public static final Integer IN_STORAGE = 1;   // 库存
    public static final Integer ON_DISPLAY = 2;   // 展出
    public static final Integer LOST = 3;         // 丢失
    public static final Integer GIFTED = 4;       // 馈赠
    public static final Integer PUBLICATION = 5;  // 出版物
    public static final Integer AUCTION = 6;      // 拍卖
}
