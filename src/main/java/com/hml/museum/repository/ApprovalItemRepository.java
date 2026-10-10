package com.hml.museum.repository;

import com.hml.museum.entity.ApprovalItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalItemRepository extends JpaRepository<ApprovalItem, Long> {
    List<ApprovalItem> findByApprovalIdOrderByIdAsc(Long approvalId);
}
