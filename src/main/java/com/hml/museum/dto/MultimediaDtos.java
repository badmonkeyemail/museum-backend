package com.hml.museum.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 多媒体相关 DTO。
 *
 * 设计原则：
 * 1. multimedia 只允许修改 description，因此没有通用 UpdateRequest；
 * 2. 封面由前端指定某个 multimedia，后端只保证同一作品最多一个 isPrimary=1；
 * 3. objectKey 不在浏览响应中暴露给前端；前端通过 url 访问缩略图。
 */
public final class MultimediaDtos {

    private MultimediaDtos() {
    }

    /** 新增逻辑多媒体记录。 */
    public record CreateRequest(
            @NotNull(message = "多媒体类型不能为空")
            Integer mediaTypeId,

            @Size(max = 200, message = "说明长度不能超过200")
            String description,

            @Size(max = 2000, message = "地址长度不能超过2000")
            String address,

            @Min(value = 0, message = "排序值不能小于0")
            Integer sortOrder

    ) {
        public CreateRequest {
            if (sortOrder == null) {
                sortOrder = 1000;
            }
        }
    }

    /** 只修改 multimedia.description。 */
    public record UpdateDescriptionRequest(
            @Size(max = 200, message = "说明长度不能超过200")
            String description
    ) {
    }

    /** 前端选择某个 multimedia 作为封面时使用的响应。 */
    public record CoverResponse(
            Long artworkId,
            Long multimediaId,
            String mediaType,
            String url,
            String fallbackType
    ) {
    }


    /** 获取 RustFS 预签名 PUT URL。 */
    public record UploadUrlRequest(
            @NotBlank(message = "变体类型不能为空")
            String variantType,

            @NotBlank(message = "Content-Type不能为空")
            String contentType,

            @NotBlank(message = "文件名不能为空")
            String fileName
    ) {
    }

    /** 浏览器上传完成后回调后端。 */
    public record CompleteVariantRequest(
            Long fileSize,
            Integer width,
            Integer height,
            String checksumSha256
    ) {
    }

    /** 前端浏览媒体时使用的文件版本信息。 */
    public record VariantResponse(
            Long id,
            String variantType,
            String contentType,
            String fileName,
            Long fileSize,
            Integer width,
            Integer height,
            String checksumSha256,
            String url
    ) {
    }

    /** 多媒体完整浏览响应。 */
    public record Response(
            Long id,
            Long artworkId,
            Integer mediaTypeId,
            String description,
            String address,
            Integer sortOrder,
            Integer isPrimary,
            Integer status,
            List<VariantResponse> variants
    ) {
    }

    public record UploadUrlResponse(
            Long variantId,
            String objectKey,
            String uploadUrl
    ) {
    }
}
