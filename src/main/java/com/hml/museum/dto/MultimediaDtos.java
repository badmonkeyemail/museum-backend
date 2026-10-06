package com.hml.museum.dto;

import jakarta.validation.constraints.*;

public final class MultimediaDtos {
    private MultimediaDtos() {
    }

    public record CreateRequest(Integer mediaTypeId, String description, String address, Integer sortOrder,
                                Integer primary) {
    }

    public record UploadUrlRequest(@NotBlank String variantType, @NotBlank String contentType,
                                   @NotBlank String fileName) {
    }

    public record CompleteVariantRequest(Long fileSize, Integer width, Integer height, String checksumSha256) {
    }

    public record VariantResponse(Long id, String variantType, String objectKey, String contentType, String fileName,
                                  Long fileSize, Integer width, Integer height, String checksumSha256) {
    }

    public record UploadUrlResponse(Long variantId, String objectKey, String uploadUrl) {
    }
}
