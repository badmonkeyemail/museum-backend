package com.hml.museum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hml.museum.dto.ApprovalDtos;
import com.hml.museum.entity.*;
import com.hml.museum.repository.*;
import com.hml.museum.entity.MultimediaVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ApprovalService {
    private static final int NOT_DELETED = 0;

    private final ApprovalRequestRepository approvals;
    private final ApprovalItemRepository items;
    private final ApprovalHistoryRepository histories;
    private final ArtworkRepository artworks;
    private final ImageStorageService storage;
    private final ApprovalPdfService pdfService;
    private final ObjectMapper mapper;
    private final MultimediaVariantRepository variants;
    private final MultimediaRepository multimedia;
    private final ApprovalDownloadGrantRepository downloadGrants;

    @Value("${museum.approval.download-token-hours:24}")
    private long downloadTokenHours;

    @Transactional(readOnly = true)
    public List<ApprovalDtos.Response> list(String businessType, String status) {
        List<ApprovalRequest> approvalsList;

        if (businessType != null && status != null) {
            approvalsList =
                    approvals.findByBusinessTypeAndStatusOrderByCreatedAtDesc(
                            businessType, status);
        } else if (businessType != null) {
            approvalsList =
                    approvals.findByBusinessTypeOrderByCreatedAtDesc(
                            businessType);
        } else if (status != null) {
            approvalsList =
                    approvals.findByStatusOrderByCreatedAtDesc(status);
        } else {
            approvalsList = approvals.findAll(
                    org.springframework.data.domain.Sort.by(
                            org.springframework.data.domain.Sort.Direction.DESC,
                            "createdAt"
                    )
            );
        }

        return approvalsList.stream()
                .map(a -> toResponse(
                        a,
                        items.findByApprovalIdOrderByIdAsc(a.getId())
                ))
                .toList();
    }

    @Transactional
    public ApprovalDtos.Response create(ApprovalDtos.CreateRequest r, String ip) {
        validateBusinessType(r.businessType());
        List<ApprovalDtos.ItemRequest> requested = r.items();
        for (var item : requested) validateItem(r.businessType(), item);

        ApprovalRequest a = new ApprovalRequest();
        a.setApprovalNo(nextApprovalNo());
        a.setBusinessType(r.businessType());
        a.setStatus(ApprovalDtos.PENDING);
        a.setApplicantId(r.applicantId());
        a.setRequestData(r.requestData());
        a.setRemark(r.remark());
        a = approvals.save(a);

        List<ApprovalItem> savedItems = new ArrayList<>();
        for (var item : requested) {
            ApprovalItem x = new ApprovalItem();
            x.setApprovalId(a.getId());
            x.setArtworkId(item.artworkId());
            x.setMultimediaId(item.multimediaId());
            x.setVariantId(item.variantId());
            x.setItemData(item.itemData());
            savedItems.add(items.save(x));
        }

        byte[] pdf = pdfService.generate(a, savedItems);
        String key = "approvals/" + a.getApprovalNo() + "/request.pdf";
        storage.putBytes(key, new ByteArrayInputStream(pdf), pdf.length, "application/pdf");
        a.setRequestPdfObjectKey(key);
        a = approvals.save(a);

        history(a.getId(), "SUBMIT", null, ApprovalDtos.PENDING, r.applicantId(),
                r.requestData(), key, ip);
        return toResponse(a, savedItems);
    }

    @Transactional
    public ApprovalDtos.Response decide(Long id, ApprovalDtos.DecisionRequest r, MultipartFile signedPdf, String ip) {
        ApprovalRequest a = approvals.findById(id).orElseThrow(() -> new NoSuchElementException("审批申请不存在"));
        if (!ApprovalDtos.PENDING.equals(a.getStatus())) throw new IllegalStateException("审批申请当前不是待审批状态");
        if (signedPdf == null || signedPdf.isEmpty()) throw new IllegalArgumentException("审批结果必须上传签字/盖章 PDF");

        String old = a.getStatus();
        String key = "approvals/" + a.getApprovalNo() + "/signed-" + System.currentTimeMillis() + ".pdf";
        try {
            byte[] data = signedPdf.getBytes();
            storage.putBytes(key, new ByteArrayInputStream(data), data.length, "application/pdf");
            a.setSignedPdfObjectKey(key);
            a.setSignedPdfSha256(sha256(data));
        } catch (Exception e) {
            throw new IllegalStateException("保存签字审批 PDF 失败", e);
        }
        a.setApproverId(r.approverId());
        a.setApprovalTime(LocalDateTime.now());
        a.setRemark(r.remark());
        a.setStatus(Boolean.TRUE.equals(r.approved()) ? ApprovalDtos.APPROVED : ApprovalDtos.REJECTED);
        a = approvals.save(a);
        history(a.getId(), Boolean.TRUE.equals(r.approved()) ? "APPROVE" : "REJECT",
                old, a.getStatus(), r.approverId(), r.remark(), key, ip);

        if (ApprovalDtos.APPROVED.equals(a.getStatus())
                && (ApprovalDtos.OUTBOUND.equals(a.getBusinessType())
                || ApprovalDtos.REPRODUCTION.equals(a.getBusinessType()))) {
            // 出库/高仿制作审批通过后，立即执行状态变更并记为完成。
            return complete(a.getId(), r.approverId(), ip, r.remark());
        }

        String downloadToken = null;
        LocalDateTime downloadExpiresAt = null;
        if (ApprovalDtos.APPROVED.equals(a.getStatus())
                && ApprovalDtos.HIGH_RES_DOWNLOAD.equals(a.getBusinessType())) {
            if (downloadGrants.findByApprovalId(a.getId()).isPresent()) {
                throw new IllegalStateException("该审批申请已经生成下载授权");
            }
            downloadToken = newDownloadToken();
            downloadExpiresAt = LocalDateTime.now().plusHours(downloadTokenHours);
            ApprovalDownloadGrant grant = new ApprovalDownloadGrant();
            grant.setApprovalId(a.getId());
            grant.setTokenHash(sha256(downloadToken.getBytes(StandardCharsets.UTF_8)));
            grant.setExpiresAt(downloadExpiresAt);
            grant.setStatus("ACTIVE");
            grant.setDownloadCount(0L);
            downloadGrants.save(grant);
        }
        return toResponse(a, items.findByApprovalIdOrderByIdAsc(id), downloadToken, downloadExpiresAt);
    }

    @Transactional
    public ApprovalDtos.Response complete(Long id, Long operatorId, String ip, String remark) {
        ApprovalRequest a = approvals.findById(id).orElseThrow(() -> new NoSuchElementException("审批申请不存在"));
        if (!ApprovalDtos.APPROVED.equals(a.getStatus())) throw new IllegalStateException("只有已通过审批才能执行");
        if (ApprovalDtos.HIGH_RES_DOWNLOAD.equals(a.getBusinessType()))
            throw new IllegalStateException("高清图下载审批通过后通过 Token 下载，不执行 complete()");
        if (!ApprovalDtos.OUTBOUND.equals(a.getBusinessType())
                && !ApprovalDtos.REPRODUCTION.equals(a.getBusinessType()))
            throw new IllegalStateException("该业务类型不支持 complete()");
        List<ApprovalItem> list = items.findByApprovalIdOrderByIdAsc(id);

        if (ApprovalDtos.OUTBOUND.equals(a.getBusinessType()) || ApprovalDtos.REPRODUCTION.equals(a.getBusinessType())) {
            for (ApprovalItem item : list) {
                Integer targetStatus = targetStatus(item.getItemData());
                if (targetStatus == null) throw new IllegalArgumentException("出库/制作审批明细缺少 targetStatusId");
                Artwork artwork = artworks.findByIdAndDeleted(item.getArtworkId(), NOT_DELETED)
                        .orElseThrow(() -> new NoSuchElementException("作品不存在: " + item.getArtworkId()));
                if (!Objects.equals(ArtworkStatusIds.IN_STORAGE, artwork.getStatusId()))
                    throw new IllegalStateException("作品当前已不是库存状态: " + item.getArtworkId());
                artwork.setStatusId(targetStatus);
                artworks.save(artwork);
            }
        }

        a.setStatus(ApprovalDtos.COMPLETED);
        a.setCompletedTime(LocalDateTime.now());
        a.setRemark(remark);
        a = approvals.save(a);
        history(a.getId(), "COMPLETE", ApprovalDtos.APPROVED, ApprovalDtos.COMPLETED, operatorId,
                remark, null, ip);
        return toResponse(a, list);
    }

    @Transactional(readOnly = true)
    public ApprovalDtos.Response get(Long id) {
        ApprovalRequest a = approvals.findById(id).orElseThrow(() -> new NoSuchElementException("审批申请不存在"));
        return toResponse(a, items.findByApprovalIdOrderByIdAsc(id));
    }

    @Transactional(readOnly = true)
    public List<ApprovalHistory> history(Long id) {
        approvals.findById(id).orElseThrow(() -> new NoSuchElementException("审批申请不存在"));
        return histories.findByApprovalIdOrderByCreatedAtAscIdAsc(id);
    }

    private void validateBusinessType(String type) {
        if (!ApprovalDtos.OUTBOUND.equals(type)
                && !ApprovalDtos.HIGH_RES_DOWNLOAD.equals(type)
                && !ApprovalDtos.REPRODUCTION.equals(type)) {
            throw new IllegalArgumentException("businessType 必须为 OUTBOUND/HIGH_RES_DOWNLOAD/REPRODUCTION");
        }
    }

    private void validateItem(String type, ApprovalDtos.ItemRequest r) {
        Artwork artwork = artworks.findByIdAndDeleted(r.artworkId(), NOT_DELETED)
                .orElseThrow(() -> new NoSuchElementException("作品不存在: " + r.artworkId()));
        if ((ApprovalDtos.OUTBOUND.equals(type) || ApprovalDtos.REPRODUCTION.equals(type))
                && !Objects.equals(ArtworkStatusIds.IN_STORAGE, artwork.getStatusId())) {
            throw new IllegalStateException("出库/高仿制作申请要求作品当前为库存状态: " + r.artworkId());
        }
        if ((ApprovalDtos.OUTBOUND.equals(type) || ApprovalDtos.REPRODUCTION.equals(type))
                && targetStatus(r.itemData()) == null) {
            throw new IllegalArgumentException("出库/高仿制作申请明细必须提供 itemData.targetStatusId");
        }

        if (ApprovalDtos.HIGH_RES_DOWNLOAD.equals(type)) {
            if (r.multimediaId() == null || r.variantId() == null) {
                throw new IllegalArgumentException("高清图下载必须指定 multimediaId 和 variantId");
            }
            Multimedia media = multimedia.findByIdAndArtworkIdAndDeleted(r.multimediaId(), r.artworkId(), 0)
                    .orElseThrow(() -> new IllegalArgumentException("多媒体不属于指定作品"));
            MultimediaVariant v = variants.findByIdAndDeletedAndStatus(r.variantId(), 0, 1)
                    .orElseThrow(() -> new NoSuchElementException("高清图变体不存在"));
            if (!Objects.equals(v.getMultimediaId(), media.getId())
                    || !"HIGH_RES".equals(v.getVariantType())) {
                throw new IllegalArgumentException("下载明细必须指向该作品多媒体对象的 HIGH_RES 变体");
            }
        }
    }

    private Integer targetStatus(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode n = mapper.readTree(json);
            JsonNode x = n.get("targetStatusId");
            return x == null || x.isNull() ? null : x.asInt();
        } catch (Exception e) {
            throw new IllegalArgumentException("itemData 不是合法 JSON");
        }
    }

    private String nextApprovalNo() {
        return "SP" + java.time.LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private void history(Long approvalId, String action, String from, String to, Long operator,
                         String detail, String pdfKey, String ip) {
        ApprovalHistory h = new ApprovalHistory();
        h.setApprovalId(approvalId);
        h.setAction(action);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setOperatorId(operator);
        h.setDetail(toJsonDetail(detail));
        h.setPdfObjectKey(pdfKey);
        h.setIpAddress(ip);
        histories.save(h);
    }

    @Transactional
    public List<ApprovalDtos.DownloadItemResponse> resolveDownloadToken(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("下载 Token 不能为空");
        String hash = sha256(token.getBytes(StandardCharsets.UTF_8));
        ApprovalDownloadGrant grant = downloadGrants
                .findByTokenHashAndStatusAndExpiresAtAfter(hash, "ACTIVE", LocalDateTime.now())
                .orElseThrow(() -> new NoSuchElementException("下载 Token 无效或已过期"));
        ApprovalRequest approval = approvals.findById(grant.getApprovalId())
                .orElseThrow(() -> new NoSuchElementException("下载授权对应的审批申请不存在"));
        if (!ApprovalDtos.APPROVED.equals(approval.getStatus())
                || !ApprovalDtos.HIGH_RES_DOWNLOAD.equals(approval.getBusinessType()))
            throw new IllegalStateException("该下载授权当前不可用");

        List<ApprovalDtos.DownloadItemResponse> result = items
                .findByApprovalIdOrderByIdAsc(approval.getId()).stream()
                .map(item -> {
                    MultimediaVariant variant = variants.findByIdAndDeletedAndStatus(item.getVariantId(), 0, 1)
                            .orElseThrow(() -> new NoSuchElementException("高清图文件不存在: " + item.getVariantId()));
                    if (!"HIGH_RES".equals(variant.getVariantType())
                            || !Objects.equals(variant.getMultimediaId(), item.getMultimediaId()))
                        throw new IllegalStateException("审批明细与高清图文件不匹配");
                    return new ApprovalDtos.DownloadItemResponse(
                            variant.getId(), variant.getFileName(), storage.createDownloadUrl(variant.getObjectKey()));
                }).toList();

        // 有效期内可重复下载；每次成功签发短时效 RustFS URL 均累计次数。
        grant.setDownloadCount(grant.getDownloadCount() + 1);
        grant.setLastDownloadedAt(LocalDateTime.now());
        downloadGrants.save(grant);
        return result;
    }

    private ApprovalDtos.Response toResponse(ApprovalRequest a, List<ApprovalItem> list) {
        return toResponse(a, list, null, null);
    }

    private ApprovalDtos.Response toResponse(ApprovalRequest a, List<ApprovalItem> list,
                                             String downloadToken, LocalDateTime downloadExpiresAt) {
        String requestUrl = a.getRequestPdfObjectKey() == null ? null : storage.createDownloadUrl(a.getRequestPdfObjectKey());
        String signedUrl = a.getSignedPdfObjectKey() == null ? null : storage.createDownloadUrl(a.getSignedPdfObjectKey());
        return new ApprovalDtos.Response(
                a.getId(), a.getApprovalNo(), a.getBusinessType(), a.getStatus(),
                a.getApplicantId(), a.getApproverId(), a.getRequestData(),
                requestUrl, signedUrl, a.getSignedPdfSha256(), a.getRemark(),
                a.getApprovalTime(), a.getCompletedTime(), a.getCreatedAt(),
                list.stream().map(x -> new ApprovalDtos.ItemResponse(
                        x.getId(), x.getArtworkId(), x.getMultimediaId(), x.getVariantId(), x.getItemData())).toList(),
                downloadToken, downloadExpiresAt
        );
    }

    private String toJsonDetail(String detail) {
        if (detail == null || detail.isBlank()) return null;
        try {
            mapper.readTree(detail);
            return detail;
        } catch (Exception ignored) {
            try {
                return mapper.writeValueAsString(detail);
            } catch (Exception e) {
                throw new IllegalStateException("审批历史 detail JSON 序列化失败", e);
            }
        }
    }

    private String newDownloadToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
