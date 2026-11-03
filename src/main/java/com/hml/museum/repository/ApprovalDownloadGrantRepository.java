package com.hml.museum.repository;

import com.hml.museum.entity.ApprovalDownloadGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ApprovalDownloadGrantRepository extends JpaRepository<ApprovalDownloadGrant, Long> {
    Optional<ApprovalDownloadGrant> findByTokenHashAndStatusAndExpiresAtAfter(
            String tokenHash, String status, LocalDateTime now);
    Optional<ApprovalDownloadGrant> findByApprovalId(Long approvalId);
}
