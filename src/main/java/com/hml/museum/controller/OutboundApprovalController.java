package com.hml.museum.controller;

import com.hml.museum.dto.OutboundDtos.*;
import com.hml.museum.entity.OutboundApproval;
import com.hml.museum.repository.OutboundApprovalRepository;
import com.hml.museum.service.OutboundApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/outbound-approvals")
@RequiredArgsConstructor
public class OutboundApprovalController {
    private final OutboundApprovalService service;
    private final OutboundApprovalRepository repo;

    @PostMapping
    public OutboundApproval create(@Valid @RequestBody CreateRequest r) {
        return service.create(r);
    }

    @GetMapping
//    public List<OutboundApproval> list(@RequestParam(required = false) String status) {
//        return status == null ? repo.findAll() : repo.findByApprovalStatusOrderByCreatedAtDesc(status);
//    }
    public List<OutboundApproval> list(@RequestParam(required = false) Integer statusId) {
        return statusId == null ? repo.findAll() : repo.findByApprovalStatusOrderByCreatedAtDesc(statusId);
    }

    @PostMapping("/{id}/approve")
    public OutboundApproval approve(@PathVariable Long id, @Valid @RequestBody ApproveRequest r) {
        return service.approve(id, r);
    }

    @PostMapping("/{id}/execute")
    public OutboundApproval execute(@PathVariable Long id, @RequestParam Integer operatorId, @RequestParam(required = false) String ipAddress) {
        return service.execute(id, operatorId, ipAddress);
    }

    @PostMapping("/{id}/return")
    public OutboundApproval returnArtwork(@PathVariable Long id, @RequestParam Integer operatorId, @RequestParam(required = false) String ipAddress, @RequestParam(required = false) String remark) {
        return service.returnArtwork(id, operatorId, ipAddress, remark);
    }
}
