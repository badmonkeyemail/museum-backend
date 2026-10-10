package com.hml.museum.repository;

import com.hml.museum.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    Optional<ApprovalRequest> findByApprovalNo(String approvalNo);
    List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<ApprovalRequest> findByBusinessTypeAndStatusOrderByCreatedAtDesc(String businessType, String status);
}
