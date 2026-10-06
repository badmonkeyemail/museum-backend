package com.hml.museum.dto;

import com.hml.museum.service.SearchFieldType;
import com.hml.museum.service.SearchOperator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 作品查询相关 DTO。
 * 作品查询请求、条件和字段元数据统一定义于本类。
 */
public final class ArtworkSearchDtos {

    private ArtworkSearchDtos() {
    }

    /** 前端查询字段元数据。 */
    public record FieldMetadata(
            String field,
            String display,
            SearchFieldType type,
            List<SearchOperator> operators,
            String source
    ) {
    }

    /**
     * 一个查询条件。
     *
     * 普通字段使用 value1，value2 必须为空。
     * DATE + BETWEEN 使用 value1/value2。
     */
    public record Condition(
            @NotBlank(message = "查询字段不能为空")
            String field,

            @NotNull(message = "查询操作符不能为空")
            SearchOperator operator,

            String value1,
            String value2
    ) {
    }

    /**
     * 查询请求：当前最多两个条件，多个条件固定使用 AND。
     */
    public record SearchRequest(
            @Valid
            @NotEmpty(message = "至少需要一个查询条件")
            @Size(max = 2, message = "最多支持两个查询条件")
            List<Condition> conditions,

            @Min(value = 0, message = "page不能小于0")
            Integer page,

            @Min(value = 1, message = "size不能小于1")
            @Max(value = 200, message = "size不能超过200")
            Integer size
    ) {
        public SearchRequest {
            if (page == null) page = 0;
            if (size == null) size = 20;
        }
    }
}
