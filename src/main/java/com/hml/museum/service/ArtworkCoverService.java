package com.hml.museum.service;

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
 * <p>作品表不保存图片字段。封面来源于：
 * artwork -> multimedia(is_primary=1, PHOTO) -> multimedia_variant。</p>
 * <p>作品列表使用 THUMB_64，作品详情使用 THUMB_256。</p>
 */
@Service
@RequiredArgsConstructor
public class ArtworkCoverService {

    private static final int ACTIVE = 1;
    private static final int NOT_DELETED = 0;

    private final MultimediaRepository multimediaRepository;
    private final MultimediaVariantRepository variantRepository;
    private final ImageStorageService storage;

    @Transactional(readOnly = true)
    public String getCoverUrl(Long artworkId, String variantType) {
        Multimedia media = multimediaRepository
                .findFirstByArtworkIdAndPrimaryAndMultimediaTypeIdAndDeletedAndStatusOrderBySortOrderAscIdAsc(
                        artworkId, 1, MediaTypeIds.PHOTO, NOT_DELETED, ACTIVE
                )
                .orElse(null);

        if (media == null) {
            return null;
        }

        return variantRepository
                .findByMultimediaIdAndVariantTypeAndDeletedAndStatus(
                        media.getId(), variantType, NOT_DELETED, ACTIVE
                )
                .map(MultimediaVariant::getObjectKey)
                .map(storage::createDownloadUrl)
                .orElse(null);
    }

    /** 批量解析封面，避免作品列表出现 N+1 查询。 */
    @Transactional(readOnly = true)
    public Map<Long, String> getCoverUrls(
            Collection<Long> artworkIds,
            String variantType
    ) {
        if (artworkIds == null || artworkIds.isEmpty()) {
            return Map.of();
        }

        List<Multimedia> mediaList = multimediaRepository
                .findByArtworkIdInAndPrimaryAndMultimediaTypeIdAndDeletedAndStatusOrderBySortOrderAscIdAsc(
                        artworkIds, 1, MediaTypeIds.PHOTO, NOT_DELETED, ACTIVE
                );

        if (mediaList.isEmpty()) {
            return Map.of();
        }

        // 理论上每个作品只有一个 primary=1；这里对旧数据做防御处理。
        Map<Long, Multimedia> primaryByArtwork = new LinkedHashMap<>();
        for (Multimedia media : mediaList) {
            primaryByArtwork.putIfAbsent(media.getArtworkId(), media);
        }

        List<Long> multimediaIds = primaryByArtwork.values().stream()
                .map(Multimedia::getId)
                .toList();

        Map<Long, MultimediaVariant> variantByMedia = variantRepository
                .findByMultimediaIdInAndVariantTypeAndDeletedAndStatus(
                        multimediaIds, variantType, NOT_DELETED, ACTIVE
                )
                .stream()
                .collect(Collectors.toMap(
                        MultimediaVariant::getMultimediaId,
                        Function.identity(),
                        (a, b) -> a
                ));

        Map<Long, String> result = new LinkedHashMap<>();
        for (Map.Entry<Long, Multimedia> entry : primaryByArtwork.entrySet()) {
            MultimediaVariant variant = variantByMedia.get(entry.getValue().getId());
            if (variant != null && variant.getObjectKey() != null) {
                result.put(
                        entry.getKey(),
                        storage.createDownloadUrl(variant.getObjectKey())
                );
            }
        }
        return result;
    }
}
