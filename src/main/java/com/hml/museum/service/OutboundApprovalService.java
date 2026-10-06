package com.hml.museum.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hml.museum.dto.OutboundDtos;
import com.hml.museum.entity.Artwork;
import com.hml.museum.entity.ArtworkHistory;
import com.hml.museum.entity.OutboundApproval;
import com.hml.museum.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class OutboundApprovalService {
    private final OutboundApprovalRepository repo;
    private final ArtworkRepository artworks;
    private final ArtworkStatusRepository statuses;
    private final ArtworkHistoryRepository history;
    private final ObjectMapper mapper;

    @Transactional
    public OutboundApproval create(OutboundDtos.CreateRequest r) {
        Artwork a = artworks.findByIdAndDeleted(r.artworkId(), 0).orElseThrow(() -> new NoSuchElementException("作品不存在"));
        if (!ArtworkStatusIds.IN_STORAGE.equals(a.getStatusId()))
            throw new IllegalStateException("作品当前不是库存状态，不能申请出库");
        statuses.findById(r.targetStatusId()).orElseThrow(() -> new IllegalArgumentException("目标状态不存在"));
        if (ArtworkStatusIds.GIFTED.equals(r.targetStatusId()) && r.returnTime() != null)
            throw new IllegalArgumentException("馈赠不应设置归还时间");
        OutboundApproval x = new OutboundApproval();
        x.setArtworkId(r.artworkId());
        x.setTargetStatusId(r.targetStatusId());
        x.setExplanation(r.explanation());
        x.setOutboundTime(r.outboundTime());
        //x.setExpectedReturnTime();
        x.setReturnTime(r.returnTime());
        x.setApplicantId(r.applicantId());
        x.setRemark(r.remark());
        x.setApprovalStatus(0);
        return repo.save(x);
    }

    @Transactional
    public OutboundApproval approve(Long id, OutboundDtos.ApproveRequest r) {
        OutboundApproval x = repo.findById(id).orElseThrow();
        //if (!"PENDING".equals(x.getApprovalStatus())) throw new IllegalStateException("申请不在待审批状态");
        x.setApproverId(r.approverId());
        x.setApprovalTime(LocalDateTime.now());
        x.setRemark(r.remark());
        //x.setApprovalStatus(Boolean.TRUE.equals(r.approved()) ? "APPROVED" : "REJECTED");
        x.setApprovalStatus(Boolean.TRUE.equals(r.approved()) ? 1 : 2);
        return repo.save(x);
    }

    @Transactional
    public OutboundApproval execute(Long id, Integer operatorId, String ip) {
        OutboundApproval x = repo.findById(id).orElseThrow();
        if (x.getApprovalStatus() != 1) throw new IllegalStateException("出库审批尚未通过");
        Artwork a = artworks.findByIdAndDeleted(x.getArtworkId(), 0).orElseThrow();
        if (!Long.valueOf(ArtworkStatusIds.IN_STORAGE).equals(a.getStatusId().longValue()))
            throw new IllegalStateException("作品当前已不是库存状态");
        String before = snapshot(a);
        a.setStatusId(x.getTargetStatusId());
        artworks.save(a);
        x.setApprovalStatus(3);
        if (x.getOutboundTime() == null) x.setOutboundTime(LocalDateTime.now());
        repo.save(x);
        saveHistory(a, "OUTBOUND_EXECUTE", operatorId, x.getId(), "执行出库", before, snapshot(a), ip);
        return x;
    }

    @Transactional
    public OutboundApproval returnArtwork(Long id, Integer operatorId, String ip, String remark) {
        OutboundApproval x = repo.findById(id).orElseThrow();
        if (x.getTargetStatusId() != ArtworkStatusIds.ON_DISPLAY)
            throw new IllegalStateException("只有展出类型出库可以普通归还");
        Artwork a = artworks.findByIdAndDeleted(x.getArtworkId(), 0).orElseThrow();
        String before = snapshot(a);
        a.setStatusId(ArtworkStatusIds.IN_STORAGE);
        artworks.save(a);
        x.setReturnTime(LocalDateTime.now());
        x.setRemark(remark);
        repo.save(x);
        saveHistory(a, "RETURN", operatorId, x.getId(), "作品归还入库", before, snapshot(a), ip);
        return x;
    }

    private String snapshot(Artwork a) {
        try {
            return mapper.writeValueAsString(a);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void saveHistory(Artwork a, String type, Integer op, Long outbound, String summary, String before, String after, String ip) {
        ArtworkHistory h = new ArtworkHistory();
        h.setArtworkId(a.getId());
        h.setOperationType(type);
        h.setOperatorId(op);
        h.setRelatedOutboundId(outbound);
        h.setOperationSummary(summary);
        h.setOldData(before);
        h.setNewData(after);
        h.setIpAddress(ip);
        history.save(h);
    }
}
