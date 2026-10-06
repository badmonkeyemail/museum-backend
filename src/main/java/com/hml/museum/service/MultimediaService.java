package com.hml.museum.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hml.museum.dto.MultimediaDtos;
import com.hml.museum.entity.ArtworkHistory;
import com.hml.museum.entity.Multimedia;
import com.hml.museum.entity.MultimediaVariant;
import com.hml.museum.repository.ArtworkHistoryRepository;
import com.hml.museum.repository.MultimediaRepository;
import com.hml.museum.repository.MediaTypeRepository;
import com.hml.museum.repository.MultimediaVariantRepository;
import com.hml.museum.repository.ArtworkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MultimediaService {
    private static final Set<String> PHOTO_VARIANTS = Set.of("ORIGINAL", "HIGH_RES", "THUMB_1024", "THUMB_256", "THUMB_64");
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png");
    private final MultimediaRepository multimediaRepo;
    private final MultimediaVariantRepository variantRepo;
    private final MediaTypeRepository typeRepo;
    private final ArtworkRepository artworkRepo;
    private final ImageStorageService storage;
    private final ArtworkHistoryRepository historyRepo;
    private final ObjectMapper mapper;

    @Transactional
    public Multimedia create(Long artworkId, MultimediaDtos.CreateRequest r) {
        artworkRepo.findByIdAndDeleted(artworkId, 0).orElseThrow();
        typeRepo.findById(r.mediaTypeId()).orElseThrow(() -> new IllegalArgumentException("多媒体类型不存在"));
        Multimedia x = new Multimedia();
        x.setArtworkId(artworkId);
        x.setDescription(r.description());
        x.setAddress(r.address());
        x.setMultimediaTypeId(r.mediaTypeId());
        x.setSortOrder(r.sortOrder() == null ? 1000 : r.sortOrder());
        x.setPrimary(r.primary() == null ? 0 : r.primary());
        x.setStatus(1);
        x.setDeleted(0);
        x = multimediaRepo.save(x);
        recordHistory(artworkId, "ADD_MULTIMEDIA", x.getId(), "新增多媒体", null, snapshot(x));
        return x;
    }

    @Transactional(readOnly = true)
    public List<Multimedia> list(Long artworkId) {
        return multimediaRepo.findByArtworkIdAndDeletedOrderBySortOrderAscIdAsc(artworkId, 0);
    }

    @Transactional(readOnly = true)
    public List<MultimediaVariant> variants(Long multimediaId) {
        return variantRepo.findByMultimediaIdAndDeletedOrderByVariantTypeAsc(multimediaId, 0);
    }

    @Transactional
    public MultimediaDtos.UploadUrlResponse createUploadUrl(Long multimediaId, MultimediaDtos.UploadUrlRequest r) {
        Multimedia m = multimediaRepo.findByIdAndDeleted(multimediaId, 0).orElseThrow();
        boolean photo = Integer.valueOf(1).equals(m.getMultimediaTypeId());
        if (!photo) throw new IllegalArgumentException("当前接口仅用于照片文件上传");
        if (!PHOTO_VARIANTS.contains(r.variantType()))
            throw new IllegalArgumentException("照片只允许 ORIGINAL/HIGH_RES/THUMB_1024/THUMB_256/THUMB_64");
        if (!IMAGE_TYPES.contains(r.contentType().toLowerCase()))
            throw new IllegalArgumentException("照片只允许 JPG/PNG");
        String ext = r.contentType().equalsIgnoreCase("image/png") ? "png" : "jpg";
        String objectKey = "artwork/" + m.getArtworkId() + "/multimedia/" + m.getId() + "/" + r.variantType().toLowerCase() + "." + ext;
        MultimediaVariant v = variantRepo.findByMultimediaIdAndVariantType(multimediaId, r.variantType())
                .orElseGet(MultimediaVariant::new);
        v.setMultimediaId(multimediaId);
        v.setVariantType(r.variantType());
        v.setObjectKey(objectKey);
        v.setContentType(r.contentType());
        v.setFileName(r.fileName());
        v.setDeleted(0);
        v.setStatus(1);
        v = variantRepo.save(v);
        return new MultimediaDtos.UploadUrlResponse(v.getId(), objectKey, storage.createUploadUrl(objectKey));
    }

    @Transactional
    public MultimediaVariant complete(Long variantId, MultimediaDtos.CompleteVariantRequest r) {
        MultimediaVariant v = variantRepo.findById(variantId).orElseThrow();
        String before = snapshot(v);
        v.setFileSize(r.fileSize());
        v.setWidth(r.width());
        v.setHeight(r.height());
        v.setChecksumSha256(r.checksumSha256());
        v = variantRepo.save(v);
        Multimedia m = multimediaRepo.findByIdAndDeleted(v.getMultimediaId(), 0).orElseThrow();
        recordHistory(m.getArtworkId(), "COMPLETE_MEDIA_VARIANT", m.getId(), "完成媒体文件登记", before, snapshot(v));
        return v;
    }

    @Transactional
    public void deleteVariant(Long variantId) {
        MultimediaVariant v = variantRepo.findById(variantId).orElseThrow();
        String before = snapshot(v);
        v.setDeleted(1);
        v.setStatus(0);
        variantRepo.save(v);
        storage.delete(v.getObjectKey());
        Multimedia m = multimediaRepo.findByIdAndDeleted(v.getMultimediaId(), 0).orElseThrow();
        recordHistory(m.getArtworkId(), "DELETE_MEDIA_VARIANT", m.getId(), "删除媒体文件版本", before, null);
    }

    private String snapshot(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void recordHistory(Long artworkId, String type, Long multimediaId, String summary, String before, String after) {
        ArtworkHistory h = new ArtworkHistory();
        h.setArtworkId(artworkId);
        h.setOperationType(type);
        h.setRelatedMultimediaId(multimediaId);
        h.setOperationSummary(summary);
        h.setOldData(before);
        h.setNewData(after);
        historyRepo.save(h);
    }
}
