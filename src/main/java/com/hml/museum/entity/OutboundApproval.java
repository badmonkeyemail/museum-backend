package com.hml.museum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 出库审批业务单：审批通过不等于实际出库，execute 时才变更作品状态并写历史。
 */
@Entity
@Table(name = "outbound_approval")
@Data
@NoArgsConstructor
public class OutboundApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artwork_id", nullable = false)
    private Long artworkId;

    @Column(name = "target_status_id", nullable = false)
    private Integer targetStatusId;

    @Column(name = "approval_status", nullable = false)
    private Integer approvalStatus; // 0待审 1通过 2驳回 3已出库 4已归还

    @Column(length = 4000)
    private String explanation;

    @Column(name = "outbound_time")
    private LocalDateTime outboundTime;

    @Column(name = "expected_return_time")
    private LocalDateTime expectedReturnTime;

    @Column(name = "actual_return_time")
    private LocalDateTime returnTime;

    @Column(name = "applicant_id", nullable = false)
    private Integer applicantId;

    @Column(name = "approver_id")
    private Integer approverId;

    @Column(name = "approval_time")
    private LocalDateTime approvalTime;

    @Column(length = 4000)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
