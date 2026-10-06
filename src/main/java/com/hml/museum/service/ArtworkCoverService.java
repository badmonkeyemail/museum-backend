package com.hml.museum.service;

import com.hml.museum.dto.MultimediaDtos;
import com.hml.museum.entity.Multimedia;
import com.hml.museum.entity.MultimediaVariant;
import com.hml.museum.repository.MultimediaRepository;
import com.hml.museum.repository.MultimediaVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 作品封面解析服务。
 *
 * <p>作品表不保存图片字段。封面来源于 multimedia.is_primary=1。
 * is_primary 只表示“该 multimedia 是作品封面来源”，与媒体类型无关。</p>
 *
 * <ul>
 *     <li>PHOTO：使用 THUMB_64 / THUMB_256 等照片缩略图；</li>
 *     <li>VIDEO：优先使用 VIDEO_POSTER；没有海报时返回 VIDEO_ICON；</li>
 *     <li>TEXT：不返回文件 URL，前端使用 TEXT_ICON。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ArtworkCoverService {

    private static final int ACTIVE = 1;
    private static final int NOT_DELETED = 0;

    private final MultimediaRepository multimediaRepository;
    private final MultimediaVariantRepository variantRepository;
    private final ImageStorageService storage;

    /**
     * 解析单个作品封面。
     *
     * @param artworkId 作品 ID
     * @param variantType PHOTO 时需要的缩略图类型，例如 THUMB_64 / THUMB_256
     */
    @Transactional(readOnly = true)
    public MultimediaDtos.CoverResponse resolve(
            Long artworkId,
            String variantType
    ) {
        Multimedia media = multimediaRepository
                .findByArtworkIdAndPrimaryAndDeleted(
                        artworkId,
                        1,
                        NOT_DELETED
                )
                .filter(m -> ACTIVE == m.getStatus())
                .orElse(null);

        return media == null
                ? null
                : buildCover(media, variantType);
    }

    /**
     * 批量解析作品封面，避免作品列表出现 N+1 查询。
     */
    @Transactional(readOnly = true)
    public Map<Long, MultimediaDtos.CoverResponse> resolveBatch(
            Collection<Long> artworkIds,
            String variantType
    ) {
        if (artworkIds == null || artworkIds.isEmpty()) {
            return Map.of();
        }

        List<Multimedia> mediaList =
                multimediaRepository.findByArtworkIdInAndPrimaryAndDeleted(
                        artworkIds,
                        1,
                        NOT_DELETED
                );

        if (mediaList.isEmpty()) {
            return Map.of();
        }

        // 理论上每个作品只有一个 primary=1；这里对旧数据做防御处理。
        Map<Long, Multimedia> primaryByArtwork = new LinkedHashMap<>();
        for (Multimedia media : mediaList) {
            if (ACTIVE == media.getStatus()) {
                primaryByArtwork.putIfAbsent(media.getArtworkId(), media);
            }
        }

        if (primaryByArtwork.isEmpty()) {
            return Map.of();
        }

        List<Long> multimediaIds = primaryByArtwork.values().stream()
                .map(Multimedia::getId)
                .toList();

        /*
         * PHOTO 需要按 variantType 查询；
         * VIDEO 需要 VIDEO_POSTER；
         * TEXT 不需要查询文件版本。
         *
         * 这里一次性把所有有效版本取回，再在内存中按
         * multimediaId + variantType 建索引，避免 N+1。
         */
        List<MultimediaVariant> variants =
                variantRepository
                        .findByMultimediaIdInAndDeletedAndStatusOrderByMultimediaIdAscVariantTypeAsc(
                                multimediaIds,
                                NOT_DELETED,
                                ACTIVE
                        );

        Map<String, MultimediaVariant> variantByMediaAndType =
                variants.stream().collect(Collectors.toMap(
                        v -> variantKey(v.getMultimediaId(), v.getVariantType()),
                        Function.identity(),
                        (a, b) -> a
                ));

        Map<Long, MultimediaDtos.CoverResponse> result =
                new LinkedHashMap<>();

        for (Map.Entry<Long, Multimedia> entry : primaryByArtwork.entrySet()) {
            Multimedia media = entry.getValue();
            MultimediaDtos.CoverResponse cover =
                    buildCover(media, variantType, variantByMediaAndType);

            if (cover != null) {
                result.put(entry.getKey(), cover);
            }
        }

        return result;
    }

    /**
     * 保留旧接口，供已有代码兼容。
     */
    @Transactional(readOnly = true)
    public String getCoverUrl(Long artworkId, String variantType) {
        MultimediaDtos.CoverResponse cover = resolve(artworkId, variantType);
        return cover == null ? null : cover.url();
    }

    /**
     * 保留旧接口，供已有代码兼容。
     */
    @Transactional(readOnly = true)
    public Map<Long, String> getCoverUrls(
            Collection<Long> artworkIds,
            String variantType
    ) {
        Map<Long, MultimediaDtos.CoverResponse> covers =
                resolveBatch(artworkIds, variantType);

        Map<Long, String> result = new LinkedHashMap<>();
        for (Map.Entry<Long, MultimediaDtos.CoverResponse> entry : covers.entrySet()) {
            if (entry.getValue().url() != null) {
                result.put(entry.getKey(), entry.getValue().url());
            }
        }
        return result;
    }

    private MultimediaDtos.CoverResponse buildCover(
            Multimedia media,
            String variantType
    ) {
        return buildCover(media, variantType, null);
    }

    private MultimediaDtos.CoverResponse buildCover(
            Multimedia media,
            String variantType,
            Map<String, MultimediaVariant> variantIndex
    ) {
        String mediaType;
        String url = null;
        String fallbackType = null;

        if (MediaTypeIds.PHOTO == media.getMultimediaTypeId()) {
            mediaType = "PHOTO";

            String normalizedVariant =
                    normalizePhotoVariant(variantType);

            MultimediaVariant variant;
            if (variantIndex != null) {
                variant = variantIndex.get(
                        variantKey(media.getId(), normalizedVariant)
                );
            } else {
                variant = variantRepository
                        .findByMultimediaIdAndVariantTypeAndDeletedAndStatus(
                                media.getId(),
                                normalizedVariant,
                                NOT_DELETED,
                                ACTIVE
                        )
                        .orElse(null);
            }

            if (variant != null && variant.getObjectKey() != null) {
                url = storage.createDownloadUrl(variant.getObjectKey());
            }

        } else if (MediaTypeIds.VIDEO == media.getMultimediaTypeId()) {
            mediaType = "VIDEO";

            MultimediaVariant poster;
            if (variantIndex != null) {
                poster = variantIndex.get(
                        variantKey(media.getId(), MultimediaVariantTypes.VIDEO_POSTER)
                );
            } else {
                poster = variantRepository
                        .findByMultimediaIdAndVariantTypeAndDeletedAndStatus(
                                media.getId(),
                                MultimediaVariantTypes.VIDEO_POSTER,
                                NOT_DELETED,
                                ACTIVE
                        )
                        .orElse(null);
            }

            if (poster != null && poster.getObjectKey() != null) {
                url = storage.createDownloadUrl(poster.getObjectKey());
            } else {
                fallbackType = "VIDEO_ICON";
            }

        } else if (MediaTypeIds.TEXT == media.getMultimediaTypeId()) {
            mediaType = "TEXT";
            fallbackType = "TEXT_ICON";

        } else {
            mediaType = "UNKNOWN";
            fallbackType = "UNKNOWN_ICON";
        }

        return new MultimediaDtos.CoverResponse(
                media.getArtworkId(),
                media.getId(),
                mediaType,
                url,
                fallbackType
        );
    }

    private String normalizePhotoVariant(String variantType) {
        if (variantType == null || variantType.isBlank()) {
            return MultimediaVariantTypes.THUMB_256;
        }

        String normalized = variantType.trim().toUpperCase();

        return switch (normalized) {
            case MultimediaVariantTypes.THUMB_64,
                 MultimediaVariantTypes.THUMB_256,
                 MultimediaVariantTypes.THUMB_1024,
                 MultimediaVariantTypes.ORIGINAL,
                 MultimediaVariantTypes.HIGH_RES -> normalized;
            default -> throw new IllegalArgumentException(
                    "不支持的照片封面版本: " + variantType
            );
        };
    }

    private String variantKey(Long multimediaId, String variantType) {
        return multimediaId + ":" + variantType;
    }
}
