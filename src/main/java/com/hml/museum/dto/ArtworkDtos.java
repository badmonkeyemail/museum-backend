package com.hml.museum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class ArtworkDtos {
    private ArtworkDtos() {
    }

    public record SaveRequest(
            @NotBlank(message = "作品名称不能为空")
            @Size(max = 200, message = "作品名称长度不能超过200")
            String name,

            Integer primaryCategoryId,
            LocalDateTime creationStartTime,
            LocalDateTime creationEndTime,
            @NotNull(message = "完好程度不能为空")
            Integer conditionId,
            @Size(max = 500, message = "作品尺寸长度不能超过500")
            String dimensions,
            @Size(max = 500, message = "作品价格长度不能超过500")
            String price,
            @Size(max = 200, message = "作者长度不能超过200")
            String author,
            @Size(max = 100, message = "作品登记号长度不能超过100")
            String registrationNo,
            String inscription,
            String summary,
            Integer statusId,
            Integer locationCategoryId,
            @Size(max = 255, message = "具体位置长度不能超过255")
            String specificLocation,
            @Size(max = 500, message = "搜索关键字长度不能超过500")
            String searchKeywords,
            List<Integer> categoryIds,
            Integer operatorId,
            String ipAddress
    ) {
    }

    /** 作品列表：返回主信息 + 封面。 */
    public record ListResponse(
            Long id,
            String name,
            String author,
            LocalDateTime creationStartTime,
            LocalDateTime creationEndTime,
            Integer conditionId,
            String price,
            Integer statusId,
            Integer primaryCategoryId,
            Integer locationCategoryId,
            String specificLocation,
            Integer version,
            CoverResponse cover
    ) {
    }

    /** 作品详情：返回完整信息 + THUMB_256 封面 + 多媒体数量。 */
    public record Response(
            Long id,
            String name,
            Integer primaryCategoryId,
            LocalDateTime creationStartTime,
            LocalDateTime creationEndTime,
            @NotNull(message = "完好程度不能为空")
            Integer conditionId,
            String dimensions,
            String price,
            String author,
            String registrationNo,
            String inscription,
            String summary,
            Integer statusId,
            Integer locationCategoryId,
            String specificLocation,
            String searchKeywords,
            Integer version,
            CoverResponse cover,
            long multimediaCount
    ) {
    }
}
