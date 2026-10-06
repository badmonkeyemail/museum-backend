package com.hml.museum.repository;

import com.hml.museum.entity.OutboundApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboundApprovalRepository extends JpaRepository<OutboundApproval, Long> {
    //List<OutboundApproval> findByArtworkIdOrderByCreatedAtDesc(Long artworkId);

    List<OutboundApproval> findByApprovalStatusOrderByCreatedAtDesc(Integer approvalStatusId);
}
