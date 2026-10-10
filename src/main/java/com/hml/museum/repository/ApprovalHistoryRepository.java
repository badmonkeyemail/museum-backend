package com.hml.museum.repository;

import com.hml.museum.entity.ApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {
    List<ApprovalHistory> findByApprovalIdOrderByCreatedAtAscIdAsc(Long approvalId);
}
