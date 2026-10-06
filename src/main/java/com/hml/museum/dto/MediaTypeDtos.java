package com.hml.museum.dto;

import jakarta.validation.constraints.*;

/**
 * 用于 Entity/Repository 和 Service 之间的数据传输
 * 可以将数据库的真实字段映射为其他名称，保护数据库
 */

public class MediaTypeDtos {

    public MediaTypeDtos() {
    }

    /** 新增请求 */
    public record CreateRequest(
            @NotBlank(message = "名称不能为空")
            @Size(max = 50, message = "名称长度不能超过50")
            String name,

            @Min(value = 0, message = "排序值不能小于0")
            Integer sortOrder,

            @Min(value = 0, message = "状态值非法")
            @Max(value = 1, message = "状态值非法")
            Integer status
    ) {
        public CreateRequest {
            if (sortOrder == null) sortOrder = 1000;
            if (status == null) status = 1;
        }
    }

    /** 修改请求（id 从路径拿，不走 body） */
    public record UpdateRequest(
            @NotBlank(message = "名称不能为空")
            @Size(max = 50, message = "名称长度不能超过50")
            String name,

            @NotNull(message = "排序值不能为空")
            @Min(value = 0, message = "排序值不能小于0")
            Integer sortOrder,

            @NotNull(message = "状态不能为空")
            @Min(value = 0, message = "状态值非法")
            @Max(value = 1, message = "状态值非法")
            Integer status
    ) {
    }

    /** 查询响应 */
    public record Response(
            Integer id,
            String name,
            Integer sortOrder,
            Integer status
    ) {
    }
}
