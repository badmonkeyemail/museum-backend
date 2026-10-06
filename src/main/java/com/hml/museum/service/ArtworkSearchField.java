package com.hml.museum.service;

import java.util.List;

/**
 * 作品可查询字段定义。
 *
 * 这里是后端的“字段白名单”。
 *
 * 前端传入的 field 必须在这里存在。
 */
public enum ArtworkSearchField {

    ID(
            "id",
            "作品ID",
            SearchFieldType.LONG,
            List.of(SearchOperator.EQUALS),
            null,
            "id"
    ),

    NAME(
            "name",
            "作品名称",
            SearchFieldType.STRING,
            List.of(
                    SearchOperator.CONTAINS,
                    SearchOperator.STARTS_WITH,
                    SearchOperator.ENDS_WITH
            ),
            null,
            "name"
    ),

    CREATION_START_TIME(
            "creationStartTime",
            "创作开始时间",
            SearchFieldType.DATE,
            List.of(
                    SearchOperator.EQUALS,
                    SearchOperator.BETWEEN
            ),
            null,
            "creationStartTime"
    ),

    CREATION_END_TIME(
            "creationEndTime",
            "创作结束时间",
            SearchFieldType.DATE,
            List.of(
                    SearchOperator.EQUALS,
                    SearchOperator.BETWEEN
            ),
            null,
            "creationEndTime"
    ),

    CONDITION_ID(
            "conditionId",
            "完好程度",
            SearchFieldType.REFERENCE,
            List.of(SearchOperator.EQUALS),
            "ARTWORK_CONDITION",
            "conditionId"
    ),

    DIMENSIONS(
            "dimensions",
            "作品尺寸",
            SearchFieldType.STRING,
            List.of(
                    SearchOperator.CONTAINS,
                    SearchOperator.STARTS_WITH,
                    SearchOperator.ENDS_WITH
            ),
            null,
            "dimensions"
    ),

    PRICE(
            "price",
            "作品价格",
            SearchFieldType.STRING,
            List.of(
                    SearchOperator.CONTAINS,
                    SearchOperator.STARTS_WITH,
                    SearchOperator.ENDS_WITH
            ),
            null,
            "price"
    ),

    AUTHOR(
            "author",
            "作者",
            SearchFieldType.STRING,
            List.of(
                    SearchOperator.CONTAINS,
                    SearchOperator.STARTS_WITH,
                    SearchOperator.ENDS_WITH
            ),
            null,
            "author"
    ),

    REGISTRATION_NO(
            "registrationNo",
            "作品登记号",
            SearchFieldType.STRING,
            List.of(
                    SearchOperator.CONTAINS,
                    SearchOperator.STARTS_WITH,
                    SearchOperator.ENDS_WITH
            ),
            null,
            "registrationNo"
    ),

    INSCRIPTION(
            "inscription",
            "作品题跋",
            SearchFieldType.STRING,
            List.of(SearchOperator.CONTAINS),
            null,
            "inscription"
    ),

    SUMMARY(
            "summary",
            "作品简述",
            SearchFieldType.STRING,
            List.of(SearchOperator.CONTAINS),
            null,
            "summary"
    ),

    STATUS_ID(
            "statusId",
            "作品状态",
            SearchFieldType.REFERENCE,
            List.of(SearchOperator.EQUALS),
            "ARTWORK_STATUS",
            "statusId"
    ),

    PRIMARY_CATEGORY_ID(
            "primaryCategoryId",
            "作品分类",
            SearchFieldType.REFERENCE,
            List.of(SearchOperator.EQUALS),
            "ARTWORK_CATEGORY",
            "primaryCategoryId"
    ),

    LOCATION_CATEGORY_ID(
            "locationCategoryId",
            "位置分类",
            SearchFieldType.REFERENCE,
            List.of(SearchOperator.EQUALS),
            "LOCATION_CATEGORY",
            "locationCategoryId"
    ),

    SPECIFIC_LOCATION(
            "specificLocation",
            "具体位置",
            SearchFieldType.STRING,
            List.of(
                    SearchOperator.CONTAINS,
                    SearchOperator.STARTS_WITH,
                    SearchOperator.ENDS_WITH
            ),
            null,
            "specificLocation"
    ),

    SEARCH_KEYWORDS(
            "searchKeywords",
            "搜索关键字",
            SearchFieldType.STRING,
            List.of(SearchOperator.CONTAINS),
            null,
            "searchKeywords"
    );


    private final String field;
    private final String display;
    private final SearchFieldType type;
    private final List<SearchOperator> operators;
    private final String source;
    private final String javaField;


    ArtworkSearchField(
            String field,
            String display,
            SearchFieldType type,
            List<SearchOperator> operators,
            String source,
            String javaField
    ) {
        this.field = field;
        this.display = display;
        this.type = type;
        this.operators = operators;
        this.source = source;
        this.javaField = javaField;
    }


    public String getField() {
        return field;
    }

    public String getDisplay() {
        return display;
    }

    public SearchFieldType getType() {
        return type;
    }

    public List<SearchOperator> getOperators() {
        return operators;
    }

    public String getSource() {
        return source;
    }

    public String getJavaField() {
        return javaField;
    }


    /**
     * 根据前端 field 获取字段定义。
     */
    public static ArtworkSearchField from(String field) {

        for (ArtworkSearchField item : values()) {

            if (item.field.equals(field)) {
                return item;
            }
        }

        throw new IllegalArgumentException(
                "不支持的查询字段：" + field
        );
    }
}