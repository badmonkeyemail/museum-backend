package com.hml.museum.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hml.museum.dto.ArtworkDtos;
import com.hml.museum.entity.Artwork;
import com.hml.museum.entity.ArtworkCategoryRelation;
import com.hml.museum.entity.ArtworkCategoryRelationKey;
import com.hml.museum.repository.ArtworkCategoryRelationRepository;
import com.hml.museum.repository.ArtworkCategoryRepository;
import com.hml.museum.repository.ArtworkConditionRepository;
import com.hml.museum.repository.ArtworkHistoryRepository;
import com.hml.museum.repository.ArtworkRepository;
import com.hml.museum.repository.ArtworkStatusRepository;
import com.hml.museum.repository.LocationCategoryRepository;
import com.hml.museum.repository.MultimediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ArtworkService {

    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;
    private static final int ACTIVE = 1;

    private final ArtworkRepository artworks;
    private final ArtworkStatusRepository statuses;
    private final ArtworkConditionRepository conditions;
    private final ArtworkCategoryRepository categories;
    private final ArtworkCategoryRelationRepository relations;
    private final ArtworkHistoryRepository historyRepo;
    private final LocationCategoryRepository locationCategories;
    private final MultimediaRepository multimediaRepository;
    private final ArtworkCoverService coverService;
    private final ObjectMapper mapper;

    @Transactional
    public ArtworkDtos.Response create(ArtworkDtos.SaveRequest request) {
        validateReferences(request);
        validateCreationTimeRange(request.creationStartTime(), request.creationEndTime());

        String registrationNo = trimToNull(request.registrationNo());
        if (registrationNo != null
                && artworks.existsByRegistrationNoAndDeleted(registrationNo, NOT_DELETED)) {
            throw new IllegalStateException("作品登记号已存在: " + registrationNo);
        }

        Artwork artwork = new Artwork();
        apply(artwork, request);

        Artwork saved = artworks.save(artwork);
        replaceCategories(saved.getId(), request.categoryIds(), request.primaryCategoryId());

        record(
                saved,
                "CREATE",
                request.operatorId(),
                null,
                null,
                "新增作品",
                null,
                snapshot(saved),
                request.ipAddress()
        );

        return toDetailResponse(saved);
    }

    @Transactional
    public ArtworkDtos.Response update(Long id, ArtworkDtos.SaveRequest request) {
        validateReferences(request);
        validateCreationTimeRange(request.creationStartTime(), request.creationEndTime());

        Artwork artwork = artworks.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new NoSuchElementException("作品不存在"));

        String registrationNo = trimToNull(request.registrationNo());
        String oldRegistrationNo = trimToNull(artwork.getRegistrationNo());

        if (!Objects.equals(oldRegistrationNo, registrationNo)
                && registrationNo != null
                && artworks.existsByRegistrationNoAndDeleted(registrationNo, NOT_DELETED)) {
            throw new IllegalStateException("作品登记号已存在: " + registrationNo);
        }

        String before = snapshot(artwork);
        apply(artwork, request);

        Artwork saved = artworks.save(artwork);
        replaceCategories(id, request.categoryIds(), request.primaryCategoryId());

        record(
                saved,
                "UPDATE",
                request.operatorId(),
                null,
                null,
                "修改作品档案",
                before,
                snapshot(saved),
                request.ipAddress()
        );

        return toDetailResponse(saved);
    }

    /** 作品详情：完整作品信息 + THUMB_256 封面 + 有效多媒体数量。 */
    @Transactional(readOnly = true)
    public ArtworkDtos.Response get(Long id) {
        Artwork artwork = artworks.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new NoSuchElementException("作品不存在"));

        return toDetailResponse(artwork);
    }

    /** 作品分页列表：基本信息 + THUMB_64 封面，批量解析避免 N+1。 */
    @Transactional(readOnly = true)
    public Page<ArtworkDtos.ListResponse> page(Pageable pageable) {
        Page<Artwork> page = artworks.findByDeleted(NOT_DELETED, pageable);

        List<Long> artworkIds = page.getContent().stream()
                .map(Artwork::getId)
                .toList();

        Map<Long, com.hml.museum.dto.MultimediaDtos.CoverResponse> covers =
                coverService.resolveBatch(artworkIds, MultimediaVariantTypes.THUMB_64);

        List<ArtworkDtos.ListResponse> content = page.getContent().stream()
                .map(a -> toListResponse(a, covers.get(a.getId())))
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Transactional
    public void delete(Long id, Integer operatorId, String ipAddress) {
        Artwork artwork = artworks.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new NoSuchElementException("作品不存在"));

        String before = snapshot(artwork);
        artwork.setDeleted(DELETED);
        artworks.save(artwork);

        record(
                artwork,
                "DELETE",
                operatorId,
                null,
                null,
                "逻辑删除作品",
                before,
                snapshot(artwork),
                ipAddress
        );
    }

    private ArtworkDtos.Response toDetailResponse(Artwork artwork) {
        var cover =
                coverService.resolve(artwork.getId(), MultimediaVariantTypes.THUMB_256);

        long multimediaCount =
                multimediaRepository.countByArtworkIdAndDeletedAndStatus(
                        artwork.getId(),
                        NOT_DELETED,
                        ACTIVE
                );

        return new ArtworkDtos.Response(
                artwork.getId(),
                artwork.getName(),
                artwork.getPrimaryCategoryId(),
                artwork.getCreationStartTime(),
                artwork.getCreationEndTime(),
                artwork.getConditionId(),
                artwork.getDimensions(),
                artwork.getPrice(),
                artwork.getAuthor(),
                artwork.getRegistrationNo(),
                artwork.getInscription(),
                artwork.getSummary(),
                artwork.getStatusId(),
                artwork.getLocationCategoryId(),
                artwork.getSpecificLocation(),
                artwork.getSearchKeywords(),
                artwork.getVersion(),
                cover,
                multimediaCount
        );
    }

    private ArtworkDtos.ListResponse toListResponse(
            Artwork artwork,
            com.hml.museum.dto.MultimediaDtos.CoverResponse cover
    ) {
        return new ArtworkDtos.ListResponse(
                artwork.getId(),
                artwork.getName(),
                artwork.getAuthor(),
                //artwork.getCreationStartTime(),
                //artwork.getCreationEndTime(),
                artwork.getConditionId(),
                //artwork.getPrice(),
                artwork.getStatusId(),
                artwork.getPrimaryCategoryId(),
                artwork.getLocationCategoryId(),
                artwork.getSpecificLocation(),
                //artwork.getVersion(),
                cover
        );
    }

    private void validateCreationTimeRange(
            LocalDateTime start,
            LocalDateTime end
    ) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("创作开始时间不能晚于创作结束时间");
        }
    }

    private void validateReferences(ArtworkDtos.SaveRequest request) {
        if (request.statusId() != null) {
            statuses.findById(request.statusId())
                    .orElseThrow(() -> new IllegalArgumentException("作品状态不存在"));
        }

        if (request.conditionId() != null) {
            conditions.findById(request.conditionId())
                    .orElseThrow(() -> new IllegalArgumentException("完好程度不存在"));
        }

        if (request.primaryCategoryId() != null) {
            categories.findByIdAndStatus(request.primaryCategoryId(), ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("主分类不存在或已停用"));
        }

        if (request.locationCategoryId() != null) {
            locationCategories.findByIdAndStatus(request.locationCategoryId(), ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("位置分类不存在或已停用"));
        }
    }

    private void apply(Artwork artwork, ArtworkDtos.SaveRequest request) {
        artwork.setName(request.name().trim());
        artwork.setPrimaryCategoryId(request.primaryCategoryId());
        artwork.setCreationStartTime(request.creationStartTime());
        artwork.setCreationEndTime(request.creationEndTime());
        artwork.setConditionId(request.conditionId());
        artwork.setDimensions(request.dimensions());
        artwork.setPrice(request.price());
        artwork.setAuthor(request.author());
        artwork.setRegistrationNo(trimToNull(request.registrationNo()));
        artwork.setInscription(request.inscription());
        artwork.setSummary(request.summary());
        artwork.setStatusId(
                request.statusId() == null
                        ? ArtworkStatusIds.IN_STORAGE
                        : request.statusId()
        );
        artwork.setLocationCategoryId(request.locationCategoryId());
        artwork.setSpecificLocation(request.specificLocation());
        artwork.setSearchKeywords(request.searchKeywords());
        artwork.setFulltextContent(buildFulltextContent(artwork));
    }

    private String buildFulltextContent(Artwork artwork) {
        return Stream.of(
                        artwork.getName(),
                        artwork.getAuthor(),
                        artwork.getInscription(),
                        artwork.getSummary(),
                        artwork.getSearchKeywords()
                )
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private void replaceCategories(
            Long artworkId,
            List<Integer> ids,
            Integer primaryId
    ) {
        relations.deleteByIdArtworkId(artworkId);

        LinkedHashSet<Integer> unique =
                new LinkedHashSet<>(ids == null ? List.of() : ids);

        if (primaryId != null) {
            unique.add(primaryId);
        }

        for (Integer categoryId : unique) {
            categories.findByIdAndStatus(categoryId, ACTIVE)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "分类不存在或已停用: " + categoryId
                            ));

            ArtworkCategoryRelation relation =
                    new ArtworkCategoryRelation();

            relation.setId(
                    new ArtworkCategoryRelationKey(
                            artworkId,
                            categoryId
                    )
            );

            relation.setPrimary(
                    Objects.equals(categoryId, primaryId)
                            ? 1
                            : 0
            );

            relations.save(relation);
        }
    }

    private String snapshot(Artwork artwork) {
        try {
            return mapper.writeValueAsString(artwork);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "生成作品历史快照失败",
                    e
            );
        }
    }

    private void record(
            Artwork artwork,
            String type,
            Integer operatorId,
            Long outboundId,
            Long multimediaId,
            String summary,
            String before,
            String after,
            String ip
    ) {
        var history =
                new com.hml.museum.entity.ArtworkHistory();

        history.setArtworkId(artwork.getId());
        history.setOperationType(type);
        history.setOperatorId(operatorId);
        history.setRelatedOutboundId(outboundId);
        history.setRelatedMultimediaId(multimediaId);
        history.setOperationSummary(summary);
        history.setOldData(before);
        history.setNewData(after);
        history.setIpAddress(ip);
        historyRepo.save(history);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }
}
