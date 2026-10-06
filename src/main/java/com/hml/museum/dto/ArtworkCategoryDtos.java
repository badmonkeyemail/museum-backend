package com.hml.museum.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public final class ArtworkCategoryDtos {
    private ArtworkCategoryDtos() {
    }

    /**
     * 新增请求
     */
    public record CreateRequest(
            Integer parentId,  //null为父级节点

            @NotBlank(message = "名称不能为空")
            @Size(max = 100, message = "名称长度不能超过100")
            String name,

            //层级在添加时，应该由后端判断，不能由前端传入
//            @NotNull(message = "层级不能为空")
//            @Min(value = 1, message = "父级层级")
//            @Max(value = 5, message = "最多层级")
//            Integer level,

            //@NotNull(message = "排序不能为空")
            @Min(value = 0, message = "排序值不能小于0")
            Integer sortOrder,

            //@NotNull(message = "状态不能为空")
            @Min(value = 0, message = "状态值非法")
            @Max(value = 1, message = "状态值非法")
            Integer status
    ) {
        public CreateRequest {
            if (sortOrder == null) sortOrder = 1000;
            if (status == null) status = 1;
        }
    }

    /**
     * 修改请求（id 从路径拿，不走 body）
     */
    public record UpdateRequest(
            Integer parentId,  //null为父级节点

            @NotBlank(message = "名称不能为空")
            @Size(max = 100, message = "名称长度不能超过100")
            String name,

//            @NotNull(message = "层级不能为空")
//            @Min(value = 1, message = "父级层级")
//            @Max(value = 5, message = "最多层级")
//            Integer level,

            @NotNull(message = "排序不能为空")
            @Min(value = 0, message = "排序值不能小于0")
            Integer sortOrder,

            @NotNull(message = "状态不能为空")
            @Min(value = 0, message = "状态值非法")
            @Max(value = 1, message = "状态值非法")
            Integer status
    ) {
    }

    //单条数据
    public record Response(
            Integer id,
            Integer parentId,  //null为父级节点
            String name,
            Integer level,
            Integer sortOrder,
            Integer status
    ) {
    }

    //tree
    public record CategoryTree(
            Integer id,
            Integer parentId,
            String name,
            Integer level,
            Integer sortOrder,
            Integer status,
            List<CategoryTree> children
    ) {
    }


}
