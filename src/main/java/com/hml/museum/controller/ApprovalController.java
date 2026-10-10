package com.hml.museum.controller;

import com.hml.museum.dto.ApprovalDtos;
import com.hml.museum.entity.ApprovalHistory;
import com.hml.museum.service.ApprovalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {
    private final ApprovalService service;

    @PostMapping
    public ApprovalDtos.Response create(@Valid @RequestBody ApprovalDtos.CreateRequest request,
                                        HttpServletRequest http) {
        return service.create(request, http.getRemoteAddr());
    }

    @GetMapping("/{id}")
    public ApprovalDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/history")
    public List<ApprovalHistory> history(@PathVariable Long id) {
        return service.history(id);
    }

    @PostMapping(value = "/{id}/decision", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApprovalDtos.Response decision(
            @PathVariable Long id,
            @RequestPart("approved") Boolean approved,
            @RequestPart("approverId") Long approverId,
            @RequestPart(value = "remark", required = false) String remark,
            @RequestPart("signedPdf") MultipartFile signedPdf,
            HttpServletRequest http) {
        return service.decide(id, new ApprovalDtos.DecisionRequest(approved, approverId, remark),
                signedPdf, http.getRemoteAddr());
    }

    @PostMapping("/{id}/complete")
    public ApprovalDtos.Response complete(
            @PathVariable Long id,
            @RequestParam Long operatorId,
            @RequestParam(required = false) String remark,
            HttpServletRequest http) {
        return service.complete(id, operatorId, http.getRemoteAddr(), remark);
    }
}
