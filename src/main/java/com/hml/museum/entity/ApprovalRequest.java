package com.hml.museum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_request")
@Data
@NoArgsConstructor
public class ApprovalRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "approval_no", nullable = false, unique = true, length = 80)
    private String approvalNo;

    @Column(name = "business_type", nullable = false, length = 40)
    private String businessType;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "approver_id")
    private Long approverId;

    @Column(name = "request_data", columnDefinition = "json")
    private String requestData;

    @Column(name = "request_pdf_object_key", length = 1000)
    private String requestPdfObjectKey;

    @Column(name = "signed_pdf_object_key", length = 1000)
    private String signedPdfObjectKey;

    @Column(name = "signed_pdf_sha256", length = 64)
    private String signedPdfSha256;

    @Column(name = "approval_time")
    private LocalDateTime approvalTime;

    @Column(name = "completed_time")
    private LocalDateTime completedTime;

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
