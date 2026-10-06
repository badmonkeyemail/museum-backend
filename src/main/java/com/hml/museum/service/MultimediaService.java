package com.hml.museum.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hml.museum.dto.MultimediaDtos;
import com.hml.museum.entity.Artwork;
import com.hml.museum.entity.ArtworkHistory;
import com.hml.museum.entity.Multimedia;
import com.hml.museum.entity.MultimediaVariant;
import com.hml.museum.repository.ArtworkHistoryRepository;
import com.hml.museum.repository.ArtworkRepository;
import com.hml.museum.repository.MediaTypeRepository;
import com.hml.museum.repository.MultimediaRepository;
import com.hml.museum.repository.MultimediaVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 多媒体业务服务。
 *
 * <p>规则：</p>
 * <ul>
 *     <li>multimedia 只允许修改 description；</li>
 *     <li>封面由前端指定，后端通过锁定 artwork 行保证同一作品最多一个 is_primary=1；</li>
 *     <li>照片文件通过 RustFS Presigned PUT 直传；</li>
 *     <li>ORIGINAL/HIGH_RES/缩略图实际文件由 multimedia_variant 管理。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MultimediaService {

    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_DELETED = 1;
    private static final int NOT_DELETED = 0;

    /** 缩略图由后端生成，前端只能上传原图或高保真图。 */
    private static final Set<String> UPLOADABLE_PHOTO_VARIANTS = Set.of(
            "ORIGINAL",
            "HIGH_RES"
    );

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );

    private static final Set<String> PUBLIC_VARIANTS = Set.of(
            "THUMB_64",
            "THUMB_256",
            "THUMB_1024"
    );

    private final MultimediaRepository multimediaRepo;
    private final MultimediaVariantRepository variantRepo;
    private final MediaTypeRepository typeRepo;
    private final ArtworkRepository artworkRepo;
    private final ImageStorageService storage;
    private final ArtworkCoverService coverService;
    private final ArtworkHistoryRepository historyRepo;
    private final ObjectMapper mapper;


    // ============================================================
    // 创建
    // ============================================================

    @Transactional
    public MultimediaDtos.Response create(
            Long artworkId,
            MultimediaDtos.CreateRequest request
    ) {
        Artwork artwork = artworkRepo.findByIdAndDeleted(artworkId, NOT_DELETED)
                .orElseThrow(() -> new NoSuchElementException("作品不存在"));

        typeRepo.findByIdAndStatus(request.mediaTypeId(), STATUS_ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("多媒体类型不存在或已停用"));

        Multimedia media = new Multimedia();
        media.setArtworkId(artworkId);
        media.setDescription(trimToNull(request.description()));
        media.setAddress(trimToNull(request.address()));
        media.setMultimediaTypeId(request.mediaTypeId());
        media.setSortOrder(request.sortOrder());
        // 创建时默认不是封面；前端创建完成后通过 /{multimediaId}/cover 指定封面。
        media.setPrimary(0);
        media.setStatus(STATUS_ACTIVE);
        media.setDeleted(NOT_DELETED);

        media = multimediaRepo.save(media);

        recordHistory(
                artworkId,
                "ADD_MULTIMEDIA",
                media.getId(),
                "新增多媒体",
                null,
                snapshot(media)
        );

        return toResponse(media, loadVariants(media.getId()));
    }


    // ============================================================
    // 浏览
    // ============================================================

    @Transactional(readOnly = true)
    public List<MultimediaDtos.Response> list(Long artworkId) {

        artworkRepo.findByIdAndDeleted(artworkId, NOT_DELETED)
                .orElseThrow(() -> new NoSuchElementException("作品不存在"));

        List<Multimedia> mediaList =
                multimediaRepo.findByArtworkIdAndDeletedAndStatusOrderBySortOrderAscIdAsc(
                        artworkId,
                        NOT_DELETED,
                        STATUS_ACTIVE
                );

        if (mediaList.isEmpty()) {
            return List.of();
        }

        List<Long> multimediaIds = mediaList.stream()
                .map(Multimedia::getId)
                .toList();

        Map<Long, List<MultimediaVariant>> variantMap =
                variantRepo
                        .findByMultimediaIdInAndDeletedAndStatusOrderByMultimediaIdAscVariantTypeAsc(
                                multimediaIds,
                                NOT_DELETED,
                                STATUS_ACTIVE
                        )
                        .stream()
                        .collect(Collectors.groupingBy(
                                MultimediaVariant::getMultimediaId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        return mediaList.stream()
                .map(media -> toResponse(
                        media,
                        variantMap.getOrDefault(media.getId(), List.of())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MultimediaDtos.VariantResponse> variants(
            Long artworkId,
            Long multimediaId
    ) {
        Multimedia media = multimediaRepo
                .findByIdAndArtworkIdAndDeleted(
                        multimediaId,
                        artworkId,
                        NOT_DELETED
                )
                .orElseThrow(() -> new NoSuchElementException("多媒体不存在"));

        return loadVariants(media.getId());
    }


    // ============================================================
    // 修改 description
    // ============================================================

    @Transactional
    public MultimediaDtos.Response updateDescription(
            Long artworkId,
            Long multimediaId,
            MultimediaDtos.UpdateDescriptionRequest request
    ) {
        Multimedia media = multimediaRepo
                .findByIdAndArtworkIdAndDeleted(
                        multimediaId,
                        artworkId,
                        NOT_DELETED
                )
                .orElseThrow(() -> new NoSuchElementException("多媒体不存在"));

        String before = snapshot(media);
        media.setDescription(trimToNull(request.description()));
        Multimedia saved = multimediaRepo.save(media);

        recordHistory(
                artworkId,
                "UPDATE_MULTIMEDIA_DESCRIPTION",
                multimediaId,
                "修改多媒体说明",
                before,
                snapshot(saved)
        );

        return toResponse(saved, loadVariants(saved.getId()));
    }


    // ============================================================
    // 设置封面
    // ============================================================

    @Transactional
    public MultimediaDtos.CoverResponse setCover(
            Long artworkId,
            Long multimediaId
    ) {
        setCoverLocked(artworkId, multimediaId);
        return coverService.resolve(artworkId, MultimediaVariantTypes.THUMB_256);
    }

    /**
     * 必须在事务中执行。
     * 先锁 artwork，再清空旧封面，最后设置新封面。
     */
    private void setCoverLocked(
            Long artworkId,
            Long multimediaId
    ) {
        artworkRepo.findByIdAndDeletedForUpdate(
                        artworkId,
                        NOT_DELETED
                )
                .orElseThrow(() -> new NoSuchElementException("作品不存在"));

        Multimedia media = multimediaRepo
                .findByIdAndArtworkIdAndDeleted(
                        multimediaId,
                        artworkId,
                        NOT_DELETED
                )
                .orElseThrow(() -> new NoSuchElementException("多媒体不存在"));

        if (!Objects.equals(media.getStatus(), STATUS_ACTIVE)) {
            throw new IllegalStateException("多媒体当前不可作为封面");
        }

        // if (!Objects.equals(
        //         media.getMultimediaTypeId(),
        //         MediaTypeIds.PHOTO
        // )) {
        //     throw new IllegalArgumentException("只有照片类型可以作为作品封面");
        // }

        multimediaRepo.clearPrimaryByArtworkId(artworkId);

        media.setPrimary(1);
        multimediaRepo.save(media);
    }


    // ============================================================
    // 上传 URL
    // ============================================================

    @Transactional
    public MultimediaDtos.UploadUrlResponse createUploadUrl(
            Long artworkId,
            Long multimediaId,
            MultimediaDtos.UploadUrlRequest request
    ) {
        Multimedia media = multimediaRepo
                .findByIdAndArtworkIdAndDeleted(
                        multimediaId,
                        artworkId,
                        NOT_DELETED
                )
                .orElseThrow(() -> new NoSuchElementException("多媒体不存在"));

        if (!Objects.equals(media.getStatus(), STATUS_ACTIVE)) {
            throw new IllegalStateException("多媒体当前不可上传");
        }

        if (!Objects.equals(
                media.getMultimediaTypeId(),
                MediaTypeIds.PHOTO
        )) {
            throw new IllegalArgumentException("当前接口仅用于照片文件上传");
        }

        String variantType = request.variantType().trim().toUpperCase(Locale.ROOT);
        if (!UPLOADABLE_PHOTO_VARIANTS.contains(variantType)) {
            throw new IllegalArgumentException(
                    "客户端只能上传 ORIGINAL 或 HIGH_RES，缩略图由后端生成"
            );
        }

        String contentType = request.contentType().trim().toLowerCase(Locale.ROOT);
        if (!IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("照片只允许 JPG/PNG");
        }

        String ext = "image/png".equals(contentType) ? "png" : "jpg";
        String objectKey = buildObjectKey(
                artworkId,
                multimediaId,
                variantType,
                ext
        );

        MultimediaVariant variant =
                variantRepo.findByMultimediaIdAndVariantType(
                                multimediaId,
                                variantType
                        )
                        .orElseGet(MultimediaVariant::new);

        variant.setMultimediaId(multimediaId);
        variant.setVariantType(variantType);
        variant.setObjectKey(objectKey);
        variant.setContentType(contentType);
        variant.setFileName(request.fileName().trim());
        variant.setDeleted(NOT_DELETED);
        variant.setStatus(STATUS_ACTIVE);

        variant = variantRepo.save(variant);

        return new MultimediaDtos.UploadUrlResponse(
                variant.getId(),
                objectKey,
                storage.createUploadUrl(objectKey)
        );
    }


    // ============================================================
    // 上传完成
    // ============================================================

    @Transactional
    public MultimediaDtos.VariantResponse complete(
            Long artworkId,
            Long variantId,
            MultimediaDtos.CompleteVariantRequest request
    ) {
        MultimediaVariant variant = variantRepo.findById(variantId)
                .orElseThrow(() -> new NoSuchElementException("媒体文件版本不存在"));

        Multimedia media = multimediaRepo
                .findByIdAndArtworkIdAndDeleted(
                        variant.getMultimediaId(),
                        artworkId,
                        NOT_DELETED
                )
                .orElseThrow(() -> new NoSuchElementException("多媒体不存在"));

        if (!Objects.equals(variant.getDeleted(), NOT_DELETED)
                || !Objects.equals(variant.getStatus(), STATUS_ACTIVE)) {
            throw new IllegalStateException("媒体文件版本当前不可操作");
        }

        String before = snapshot(variant);

        var head = storage.head(variant.getObjectKey());

        if (head.contentLength() == null || head.contentLength() <= 0) {
            throw new IllegalStateException("RustFS 对象为空，上传可能未完成");
        }

        if (request.fileSize() != null
                && !Objects.equals(request.fileSize(), head.contentLength())) {
            throw new IllegalArgumentException("上传文件大小与RustFS实际文件大小不一致");
        }

        variant.setFileSize(head.contentLength());
        if (request.width() != null) {
            variant.setWidth(request.width());
        }
        if (request.height() != null) {
            variant.setHeight(request.height());
        }
        if (request.checksumSha256() != null) {
            variant.setChecksumSha256(request.checksumSha256().trim());
        }

        MultimediaVariant saved = variantRepo.save(variant);

        recordHistory(
                artworkId,
                "COMPLETE_MEDIA_VARIANT",
                media.getId(),
                "完成媒体文件上传登记",
                before,
                snapshot(saved)
        );

        return toVariantResponse(saved);
    }


    // ============================================================
    // 删除 multimedia
    // ============================================================

    @Transactional
    public void delete(
            Long artworkId,
            Long multimediaId
    ) {
        Multimedia media = multimediaRepo
                .findByIdAndArtworkIdAndDeleted(
                        multimediaId,
                        artworkId,
                        NOT_DELETED
                )
                .orElseThrow(() -> new NoSuchElementException("多媒体不存在"));

        String before = snapshot(media);

        List<MultimediaVariant> variants =
                variantRepo.findByMultimediaIdOrderByIdAsc(multimediaId);

        // 先删除 RustFS 对象，再更新数据库状态；任一步失败都不继续提交数据库删除状态。
        for (MultimediaVariant variant : variants) {
            if (variant.getObjectKey() != null) {
                storage.delete(variant.getObjectKey());
            }
        }

        for (MultimediaVariant variant : variants) {
            variant.setDeleted(STATUS_DELETED);
            variant.setStatus(0);
        }
        variantRepo.saveAll(variants);

        media.setPrimary(0);
        media.setStatus(0);
        media.setDeleted(STATUS_DELETED);
        Multimedia saved = multimediaRepo.save(media);

        recordHistory(
                artworkId,
                "DELETE_MULTIMEDIA",
                multimediaId,
                "删除多媒体及其文件版本",
                before,
                snapshot(saved)
        );
    }


    // ============================================================
    // 删除 variant
    // ============================================================

    @Transactional
    public void deleteVariant(
            Long artworkId,
            Long variantId
    ) {
        MultimediaVariant variant = variantRepo.findById(variantId)
                .orElseThrow(() -> new NoSuchElementException("媒体文件版本不存在"));

        Multimedia media = multimediaRepo
                .findByIdAndArtworkIdAndDeleted(
                        variant.getMultimediaId(),
                        artworkId,
                        NOT_DELETED
                )
                .orElseThrow(() -> new NoSuchElementException("多媒体不存在"));

        String before = snapshot(variant);

        variant.setDeleted(STATUS_DELETED);
        variant.setStatus(0);
        variantRepo.save(variant);
        storage.delete(variant.getObjectKey());

        recordHistory(
                artworkId,
                "DELETE_MEDIA_VARIANT",
                media.getId(),
                "删除媒体文件版本",
                before,
                null
        );
    }


    // ============================================================
    // DTO转换
    // ============================================================

    private List<MultimediaDtos.VariantResponse> loadVariants(Long multimediaId) {
        return variantRepo
                .findByMultimediaIdAndDeletedAndStatusOrderByVariantTypeAsc(
                        multimediaId,
                        NOT_DELETED,
                        STATUS_ACTIVE
                )
                .stream()
                .map(this::toVariantResponse)
                .toList();
    }

    private MultimediaDtos.Response toResponse(
            Multimedia media,
            List<MultimediaVariant> variants
    ) {
        return new MultimediaDtos.Response(
                media.getId(),
                media.getArtworkId(),
                media.getMultimediaTypeId(),
                media.getDescription(),
                media.getAddress(),
                media.getSortOrder(),
                media.getPrimary(),
                media.getStatus(),
                variants.stream()
                        .map(this::toVariantResponse)
                        .toList()
        );
    }

    private MultimediaDtos.VariantResponse toVariantResponse(
            MultimediaVariant variant
    ) {
        String url = PUBLIC_VARIANTS.contains(variant.getVariantType())
                ? storage.createDownloadUrl(variant.getObjectKey())
                : null;

        return new MultimediaDtos.VariantResponse(
                variant.getId(),
                variant.getVariantType(),
                variant.getContentType(),
                variant.getFileName(),
                variant.getFileSize(),
                variant.getWidth(),
                variant.getHeight(),
                variant.getChecksumSha256(),
                url
        );
    }

    private String buildObjectKey(
            Long artworkId,
            Long multimediaId,
            String variantType,
            String ext
    ) {
        return "artwork/"
                + artworkId
                + "/multimedia/"
                + multimediaId
                + "/"
                + variantType.toLowerCase(Locale.ROOT)
                + "."
                + ext;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }

    private String snapshot(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("生成历史快照失败", e);
        }
    }

    private void recordHistory(
            Long artworkId,
            String type,
            Long multimediaId,
            String summary,
            String before,
            String after
    ) {
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
