package com.hml.museum.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class ApprovalDtos {
    private ApprovalDtos() {}

    public static final String OUTBOUND = "OUTBOUND";
    public static final String HIGH_RES_DOWNLOAD = "HIGH_RES_DOWNLOAD";
    public static final String REPRODUCTION = "REPRODUCTION";

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String COMPLETED = "COMPLETED";

    public record ItemRequest(
            @NotNull Long artworkId,
            Long multimediaId,
            Long variantId,
            String itemData
    ) {}

    public record CreateRequest(
            @NotBlank String businessType,
            @NotNull Long applicantId,
            String requestData,
            @NotEmpty List<@Valid ItemRequest> items,
            String remark
    ) {}

    public record DecisionRequest(
            @NotNull Boolean approved,
            @NotNull Long approverId,
            String remark
    ) {}

    public record Response(
            Long id,
            String approvalNo,
            String businessType,
            String status,
            Long applicantId,
            Long approverId,
            String requestData,
            String requestPdfUrl,
            String signedPdfUrl,
            String signedPdfSha256,
            String remark,
            java.time.LocalDateTime approvalTime,
            java.time.LocalDateTime completedTime,
            java.time.LocalDateTime createdAt,
            List<ItemResponse> items
    ) {}

    public record ItemResponse(
            Long id,
            Long artworkId,
            Long multimediaId,
            Long variantId,
            String itemData
    ) {}
}
