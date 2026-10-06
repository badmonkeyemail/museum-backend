package com.hml.museum.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hml.museum.dto.ArtworkDtos;
import com.hml.museum.entity.Artwork;
import com.hml.museum.entity.ArtworkCategoryRelation;
import com.hml.museum.entity.ArtworkCategoryRelationKey;
import com.hml.museum.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ArtworkService {
    private final ArtworkRepository artworks;
    private final ArtworkStatusRepository statuses;
    private final ArtworkConditionRepository conditions;
    private final ArtworkCategoryRepository categories;
    private final ArtworkCategoryRelationRepository relations;
    private final ArtworkHistoryRepository historyRepo;
    private final LocationCategoryRepository locationCategories;
    private final ObjectMapper mapper;

    @Transactional
    public ArtworkDtos.Response create(ArtworkDtos.SaveRequest r) {
        validateReferences(r);
        validateCreationTimeRange(r.creationStartTime(), r.creationEndTime());
        if (artworks.existsByRegistrationNoAndDeleted(r.registrationNo().trim(), 0)) {
            throw new IllegalStateException("作品登记号已存在: " + r.registrationNo());
        }
        Artwork a = new Artwork();
        apply(a, r);
        Artwork saved = artworks.save(a);
        replaceCategories(saved.getId(), r.categoryIds(), r.primaryCategoryId());
        record(saved, "CREATE", r.operatorId(), null, null, "新增作品", null, snapshot(saved), r.ipAddress());
        return toResponse(saved);
    }

    @Transactional
    public ArtworkDtos.Response update(Long id, ArtworkDtos.SaveRequest r) {
        validateReferences(r);
        validateCreationTimeRange(r.creationStartTime(), r.creationEndTime());
        Artwork a = artworks.findByIdAndDeleted(id, 0).orElseThrow(() -> new NoSuchElementException("作品不存在"));
        if (!Objects.equals(a.getRegistrationNo(), r.registrationNo()) && artworks.existsByRegistrationNoAndDeleted(r.registrationNo().trim(), 0)) {
            throw new IllegalStateException("作品登记号已存在: " + r.registrationNo());
        }
        String before = snapshot(a);
        apply(a, r);
        Artwork saved = artworks.save(a); // @Version 防止并发编辑时旧页面覆盖新数据。
        replaceCategories(id, r.categoryIds(), r.primaryCategoryId());
        record(saved, "UPDATE", r.operatorId(), null, null, "修改作品档案", before, snapshot(saved), r.ipAddress());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ArtworkDtos.Response get(Long id) {
        return toResponse(artworks.findByIdAndDeleted(id, 0).orElseThrow(() -> new NoSuchElementException("作品不存在")));
    }

    @Transactional(readOnly = true)
    public Page<ArtworkDtos.Response> page(Pageable pageable) {
        return artworks.findByDeleted(0, pageable).map(this::toResponse);
    }

    @Transactional
    public void delete(Long id, Integer operatorId, String ipAddress) {
        Artwork a = artworks.findByIdAndDeleted(id, 0).orElseThrow(() -> new NoSuchElementException("作品不存在"));
        String before = snapshot(a);
        a.setDeleted(1);
        artworks.save(a);
        record(a, "DELETE", operatorId, null, null, "逻辑删除作品", before, snapshot(a), ipAddress);
    }

    private void validateCreationTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("创作开始时间不能晚于创作结束时间");
        }
    }

    private void validateReferences(ArtworkDtos.SaveRequest r) {
        if (r.statusId() != null)
            statuses.findById(r.statusId())
                    .orElseThrow(() -> new IllegalArgumentException("作品状态不存在"));
        if (r.conditionId() != null)
            conditions.findById(r.conditionId())
                    .orElseThrow(() -> new IllegalArgumentException("完好程度不存在"));
        if (r.primaryCategoryId() != null)
            categories.findByIdAndStatus(r.primaryCategoryId(), 1)
                    .orElseThrow(() -> new IllegalArgumentException("主分类不存在或已停用"));
        if (r.locationCategoryId() != null)
            locationCategories.findByIdAndStatus(r.locationCategoryId(), 1)
                    .orElseThrow(() -> new IllegalArgumentException("位置分类不存在或已停用"));
    }

    private void apply(Artwork a, ArtworkDtos.SaveRequest r) {
        a.setName(r.name().trim());
        a.setPrimaryCategoryId(r.primaryCategoryId());
        a.setCreationStartTime(r.creationStartTime());
        a.setCreationEndTime(r.creationEndTime());
        a.setConditionId(r.conditionId());
        a.setDimensions(r.dimensions());
        a.setPrice(r.price());
        a.setAuthor(r.author());
        a.setRegistrationNo(r.registrationNo().trim());
        a.setInscription(r.inscription());
        a.setSummary(r.summary());
        a.setStatusId(r.statusId() == null ? ArtworkStatusIds.IN_STORAGE : r.statusId());
        a.setLocationCategoryId(r.locationCategoryId());
        a.setSpecificLocation(r.specificLocation());
        a.setSearchKeywords(r.searchKeywords());
        a.setFulltextContent(buildFulltextContent(a));
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

    private void replaceCategories(Long artworkId, List<Integer> ids, Integer primaryId) {
        relations.deleteByIdArtworkId(artworkId);
        LinkedHashSet<Integer> unique = new LinkedHashSet<>(ids == null ? List.of() : ids);
        if (primaryId != null) unique.add(primaryId);
        for (Integer categoryId : unique) {
            categories.findByIdAndStatus(categoryId, 1).orElseThrow(() -> new IllegalArgumentException("分类不存在或已停用: " + categoryId));
            ArtworkCategoryRelation r = new ArtworkCategoryRelation();
            r.setId(new ArtworkCategoryRelationKey(artworkId, categoryId));
            r.setPrimary(Objects.equals(categoryId, primaryId) ? 1 : 0);
            relations.save(r);
        }
    }

    private String snapshot(Artwork a) {
        try {
            return mapper.writeValueAsString(a);
        } catch (Exception e) {
            throw new IllegalStateException("生成作品历史快照失败", e);
        }
    }

    private void record(Artwork a, String type, Integer operatorId, Long outboundId, Long multimediaId,
                        String summary, String before, String after, String ip) {
        var h = new com.hml.museum.entity.ArtworkHistory();
        h.setArtworkId(a.getId());
        h.setOperationType(type);
        h.setOperatorId(operatorId);
        h.setRelatedOutboundId(outboundId);
        h.setRelatedMultimediaId(multimediaId);
        h.setOperationSummary(summary);
        h.setOldData(before);
        h.setNewData(after);
        h.setIpAddress(ip);
        historyRepo.save(h);
    }

    private ArtworkDtos.Response toResponse(Artwork a) {
        return new ArtworkDtos.Response(a.getId(), a.getName(), a.getPrimaryCategoryId(), a.getCreationStartTime(),
                a.getCreationEndTime(), a.getConditionId(), a.getDimensions(), a.getPrice(), a.getAuthor(),
                a.getRegistrationNo(), a.getInscription(), a.getSummary(), a.getStatusId(), a.getLocationCategoryId(),
                a.getSpecificLocation(), a.getSearchKeywords(), a.getVersion());
    }
}
