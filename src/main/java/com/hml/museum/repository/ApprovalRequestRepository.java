package com.hml.museum.repository;

import com.hml.museum.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    //Optional<ApprovalRequest> findByApprovalNo(String approvalNo);
    List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<ApprovalRequest> findByBusinessTypeAndStatusOrderByCreatedAtDesc(String businessType, String status);
    List<ApprovalRequest> findByBusinessTypeOrderByCreatedAtDesc(String businessType);
}
