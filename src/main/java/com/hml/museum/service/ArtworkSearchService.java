package com.hml.museum.service;

import com.hml.museum.dto.ArtworkDtos;
import com.hml.museum.dto.ArtworkSearchDtos;
import com.hml.museum.entity.Artwork;
import com.hml.museum.repository.ArtworkRepository;
import com.hml.museum.dto.MultimediaDtos;
import jakarta.persistence.criteria.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 作品查询服务。
 *
 * <p>普通动态查询：最多两个条件，固定 AND。</p>
 * <p>全文查询：MySQL MATCH(fulltext_content)。</p>
 * <p>查询结果列表包含 THUMB_64 封面，避免作品表增加图片字段。</p>
 */
@Service
@RequiredArgsConstructor
public class ArtworkSearchService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int NOT_DELETED = 0;

    private final ArtworkRepository artworkRepository;
    private final ArtworkCoverService coverService;

    @Transactional(readOnly = true)
    public List<ArtworkSearchDtos.FieldMetadata> getFieldMetadata() {
        return java.util.Arrays.stream(ArtworkSearchField.values())
                .map(field -> new ArtworkSearchDtos.FieldMetadata(
                        field.getField(),
                        field.getDisplay(),
                        field.getType(),
                        field.getOperators(),
                        field.getSource()
                ))
                .toList();
    }

    /** 最多两个条件 AND 查询，并批量补充 THUMB_64 封面。 */
    @Transactional(readOnly = true)
    public Page<ArtworkDtos.ListResponse> search(
            ArtworkSearchDtos.SearchRequest request
    ) {
        validateSearchRequest(request);

        Specification<Artwork> specification =
                (root, query, cb) -> cb.equal(
                        root.get("deleted"), NOT_DELETED
                );

        for (ArtworkSearchDtos.Condition condition : request.conditions()) {
            specification = specification.and(buildSpecification(condition));
        }

        PageRequest pageable = PageRequest.of(
                request.page(),
                Math.min(request.size(), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<Artwork> page = artworkRepository.findAll(specification, pageable);
        List<Long> ids = page.getContent().stream().map(Artwork::getId).toList();
        Map<Long, MultimediaDtos.CoverResponse> covers =
                coverService.resolveBatch(ids, MultimediaVariantTypes.THUMB_64);

        List<ArtworkDtos.ListResponse> content = page.getContent().stream()
                .map(a -> toListResponse(a, covers.get(a.getId())))
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    /** MySQL FULLTEXT 全文查询，并批量补充 THUMB_64 封面。 */
    @Transactional(readOnly = true)
    public Page<ArtworkDtos.ListResponse> fullTextSearch(
            String keyword,
            int page,
            int size
    ) {
        if (isBlank(keyword)) {
            throw new IllegalArgumentException("全文搜索关键字不能为空");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page不能小于0");
        }

        int actualSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(
                page,
                actualSize,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<Artwork> pageResult = artworkRepository
                .fullTextSearch(keyword.trim(), pageable);

        List<Long> ids = pageResult.getContent().stream().map(Artwork::getId).toList();
        Map<Long, MultimediaDtos.CoverResponse> covers =
                coverService.getCoverUrls(ids, MultimediaVariantTypes.THUMB_64);

        List<ArtworkDtos.ListResponse> content = pageResult.getContent().stream()
                .map(a -> toListResponse(a, covers.get(a.getId())))
                .toList();

        return new PageImpl<>(content, pageable, pageResult.getTotalElements());
    }

    private Specification<Artwork> buildSpecification(
            ArtworkSearchDtos.Condition condition
    ) {
        if (condition == null) {
            throw new IllegalArgumentException("查询条件不能为空");
        }

        ArtworkSearchField field = ArtworkSearchField.from(condition.field().trim());
        SearchOperator operator = condition.operator();

        if (!field.getOperators().contains(operator)) {
            throw new IllegalArgumentException(
                    "字段[" + field.getDisplay() + "]不支持操作符[" + operator + "]"
            );
        }

        return switch (field.getType()) {
            case STRING -> buildStringSpecification(field, operator, condition.value1(), condition.value2());
            case INTEGER, LONG, REFERENCE -> buildNumberSpecification(field, operator, condition.value1(), condition.value2());
            case DATE -> buildDateSpecification(field, operator, condition.value1(), condition.value2());
        };
    }

    private Specification<Artwork> buildStringSpecification(
            ArtworkSearchField field,
            SearchOperator operator,
            String value1,
            String value2
    ) {
        requireValue1(field, value1);
        if (!isBlank(value2)) {
            throw new IllegalArgumentException("字符串字段不需要第二个查询值");
        }

        String value = escapeLike(value1.trim());
        return (root, query, cb) -> {
            Path<String> path = root.get(field.getJavaField());
            return switch (operator) {
                case CONTAINS -> cb.like(path, "%" + value + "%", '\\');
                case STARTS_WITH -> cb.like(path, value + "%", '\\');
                case ENDS_WITH -> cb.like(path, "%" + value, '\\');
                default -> throw new IllegalArgumentException(
                        "字符串字段不支持操作符：" + operator
                );
            };
        };
    }

    private Specification<Artwork> buildNumberSpecification(
            ArtworkSearchField field,
            SearchOperator operator,
            String value1,
            String value2
    ) {
        if (operator != SearchOperator.EQUALS) {
            throw new IllegalArgumentException(
                    "字段[" + field.getDisplay() + "]只支持EQUALS"
            );
        }
        requireValue1(field, value1);
        if (!isBlank(value2)) {
            throw new IllegalArgumentException("数字/引用字段不需要第二个查询值");
        }

        return switch (field.getType()) {
            case LONG -> {
                long value;
                try {
                    value = Long.parseLong(value1.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "字段[" + field.getDisplay() + "]必须输入整数"
                    );
                }
                yield (root, query, cb) -> cb.equal(root.get(field.getJavaField()), value);
            }
            case INTEGER, REFERENCE -> {
                int value;
                try {
                    value = Integer.parseInt(value1.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "字段[" + field.getDisplay() + "]必须输入整数"
                    );
                }
                yield (root, query, cb) -> cb.equal(root.get(field.getJavaField()), value);
            }
            default -> throw new IllegalStateException("不是数字/引用字段：" + field);
        };
    }

    private Specification<Artwork> buildDateSpecification(
            ArtworkSearchField field,
            SearchOperator operator,
            String value1,
            String value2
    ) {
        if (operator == SearchOperator.EQUALS) {
            requireValue1(field, value1);
            if (!isBlank(value2)) {
                throw new IllegalArgumentException("EQUALS不需要第二个日期");
            }

            LocalDate date = parseDate(field, value1);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            return (root, query, cb) -> {
                Path<LocalDateTime> path = root.get(field.getJavaField());
                return cb.and(
                        cb.greaterThanOrEqualTo(path, start),
                        cb.lessThan(path, end)
                );
            };
        }

        if (operator == SearchOperator.BETWEEN) {
            return buildDateBetweenSpecification(field, value1, value2);
        }

        throw new IllegalArgumentException(
                "日期字段不支持操作符：" + operator
        );
    }

    private Specification<Artwork> buildDateBetweenSpecification(
            ArtworkSearchField field,
            String value1,
            String value2
    ) {
        boolean blank1 = isBlank(value1);
        boolean blank2 = isBlank(value2);

        if (blank1 && blank2) {
            throw new IllegalArgumentException("BETWEEN至少需要一个日期");
        }

        if (blank1) {
            LocalDate upper = parseDate(field, value2);
            LocalDateTime end = upper.atStartOfDay();
            return (root, query, cb) ->
                    cb.lessThan(root.get(field.getJavaField()), end);
        }

        if (blank2) {
            LocalDate lower = parseDate(field, value1);
            LocalDateTime start = lower.plusDays(1).atStartOfDay();
            return (root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get(field.getJavaField()), start);
        }

        LocalDate lower = parseDate(field, value1);
        LocalDate upper = parseDate(field, value2);
        if (lower.isAfter(upper)) {
            throw new IllegalArgumentException("BETWEEN的开始日期不能晚于结束日期");
        }

        LocalDateTime start = lower.atStartOfDay();
        LocalDateTime end = upper.plusDays(1).atStartOfDay();

        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get(field.getJavaField()), start),
                cb.lessThan(root.get(field.getJavaField()), end)
        );
    }

    private LocalDate parseDate(ArtworkSearchField field, String value) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(
                    "字段[" + field.getDisplay() + "]日期不能为空"
            );
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "字段[" + field.getDisplay() + "]日期格式必须为yyyy-MM-dd"
            );
        }
    }

    private void validateSearchRequest(ArtworkSearchDtos.SearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("查询请求不能为空");
        }
        if (request.conditions() == null || request.conditions().isEmpty()) {
            throw new IllegalArgumentException("至少需要一个查询条件");
        }
        if (request.conditions().size() > 2) {
            throw new IllegalArgumentException("最多支持两个查询条件");
        }
    }

    private void requireValue1(ArtworkSearchField field, String value1) {
        if (isBlank(value1)) {
            throw new IllegalArgumentException(
                    "字段[" + field.getDisplay() + "]查询值不能为空"
            );
        }
    }

    private String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private ArtworkDtos.ListResponse toListResponse(
            Artwork a,
            MultimediaDtos.CoverResponse cover
    ) {
        return new ArtworkDtos.ListResponse(
                a.getId(),
                a.getName(),
                a.getAuthor(),
                a.getCreationStartTime(),
                a.getCreationEndTime(),
                a.getConditionId(),
                a.getPrice(),
                a.getStatusId(),
                a.getPrimaryCategoryId(),
                a.getLocationCategoryId(),
                a.getSpecificLocation(),
                a.getVersion(),
                cover
        );
    }
}
