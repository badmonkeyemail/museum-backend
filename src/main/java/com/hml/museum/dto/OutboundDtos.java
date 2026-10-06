package com.hml.museum.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class OutboundDtos {
    private OutboundDtos() {
    }

    public record CreateRequest(@NotNull Long artworkId, @NotNull Integer targetStatusId, String explanation,
                                LocalDateTime outboundTime, LocalDateTime returnTime,
                                @NotNull Integer applicantId, String remark) {
    }

    public record ApproveRequest(@NotNull Boolean approved, @NotNull Integer approverId, String remark) {
    }
}
