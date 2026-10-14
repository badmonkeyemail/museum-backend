package com.hml.museum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * 藏品和审批统计。
 *
 * 所有统计都排除逻辑删除的藏品。groupBy 字段通过白名单映射为 SQL，
 * 不接受任意 SQL 表达式，避免动态 SQL 注入。
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final JdbcTemplate jdbc;

    private record Dimension(String key, String joinSql, String idSql, String labelSql) {}

    private static final Map<String, Dimension> DIMENSIONS;

    static {
        Map<String, Dimension> m = new LinkedHashMap<>();
        m.put("category", new Dimension(
                "category",
                "", "c.id", "c.name"));
        m.put("condition", new Dimension(
                "condition",
                " LEFT JOIN artwork_condition ac ON ac.id = a.condition_id",
                "ac.id", "ac.name"));
        m.put("status", new Dimension(
                "status",
                " LEFT JOIN artwork_status ast ON ast.id = a.status_id",
                "ast.id", "ast.name"));
        m.put("location", new Dimension(
                "location",
                " LEFT JOIN location_category lc ON lc.id = a.location_category_id",
                "lc.id", "lc.name"));
        DIMENSIONS = Collections.unmodifiableMap(m);
    }

    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", one("SELECT COUNT(*) FROM artwork WHERE deleted = 0"));
        result.put("inStorage", one("SELECT COUNT(*) FROM artwork WHERE deleted = 0 AND status_id = 1"));
        result.put("onDisplay", one("SELECT COUNT(*) FROM artwork WHERE deleted = 0 AND status_id = 2"));
        result.put("lost", one("SELECT COUNT(*) FROM artwork WHERE deleted = 0 AND status_id = 3"));
        result.put("gifted", one("SELECT COUNT(*) FROM artwork WHERE deleted = 0 AND status_id = 4"));
        result.put("publication", one("SELECT COUNT(*) FROM artwork WHERE deleted = 0 AND status_id = 5"));
        result.put("auction", one("SELECT COUNT(*) FROM artwork WHERE deleted = 0 AND status_id = 6"));
        return result;
    }

    /**
     * 兼容旧接口。统计所有关联分类；若一个藏品有多个分类，会分别计入各分类。
     */
    public List<Map<String, Object>> byCategory() {
        return jdbc.queryForList("""
                SELECT c.id, c.name, c.level,
                       COUNT(DISTINCT a.id) AS artwork_count
                FROM artwork_category c
                LEFT JOIN artwork_category_relation r ON r.category_id = c.id
                LEFT JOIN artwork a ON a.id = r.artwork_id AND a.deleted = 0
                WHERE c.status = 1
                GROUP BY c.id, c.name, c.level, c.sort_order
                ORDER BY c.sort_order, c.id
                """);
    }

    public Map<String, Object> artworks(
            String groupBy,
            String categoryScope,
            Integer conditionId,
            Integer statusId,
            Integer locationCategoryId,
            LocalDate from,
            LocalDate to) {

        List<String> dimensions = parseDimensions(groupBy);
        String scope = categoryScope == null || categoryScope.isBlank()
                ? "primary" : categoryScope.trim().toLowerCase(Locale.ROOT);
        if (!scope.equals("primary") && !scope.equals("all")) {
            throw new IllegalArgumentException("categoryScope 仅支持 primary 或 all");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from 不能晚于 to");
        }

        StringBuilder joins = new StringBuilder();
        boolean categoryAdded = false;
        for (String key : dimensions) {
            Dimension d = DIMENSIONS.get(key);
            if (key.equals("category")) {
                if (!categoryAdded) {
                    if (scope.equals("all")) {
                        joins.append(" LEFT JOIN artwork_category_relation acr ON acr.artwork_id = a.id")
                             .append(" LEFT JOIN artwork_category c ON c.id = acr.category_id AND c.status = 1");
                    } else {
                        joins.append(" LEFT JOIN artwork_category c ON c.id = a.primary_category_id AND c.status = 1");
                    }
                    categoryAdded = true;
                }
            } else {
                joins.append(d.joinSql());
            }
        }

        List<String> where = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        where.add("a.deleted = 0");
        if (conditionId != null) {
            where.add("a.condition_id = ?");
            args.add(conditionId);
        }
        if (statusId != null) {
            where.add("a.status_id = ?");
            args.add(statusId);
        }
        if (locationCategoryId != null) {
            where.add("a.location_category_id = ?");
            args.add(locationCategoryId);
        }
        if (from != null) {
            where.add("a.created_at >= ?");
            args.add(from.atStartOfDay());
        }
        if (to != null) {
            where.add("a.created_at < ?");
            args.add(to.plusDays(1).atStartOfDay());
        }

        List<String> select = new ArrayList<>();
        List<String> group = new ArrayList<>();
        for (int i = 0; i < dimensions.size(); i++) {
            String key = dimensions.get(i);
            Dimension d = DIMENSIONS.get(key);
            String idSql = key.equals("category") ? "c.id" : d.idSql();
            String labelSql = key.equals("category") ? "c.name" : d.labelSql();
            select.add(idSql + " AS dim" + i + "Id");
            select.add("COALESCE(" + labelSql + ", '未设置') AS dim" + i + "Label");
            group.add(idSql);
            group.add(labelSql);
        }
        select.add("COUNT(DISTINCT a.id) AS count");

        String sql = "SELECT " + String.join(", ", select)
                + " FROM artwork a " + joins
                + " WHERE " + String.join(" AND ", where)
                + " GROUP BY " + String.join(", ", group)
                + " ORDER BY " + String.join(", ", group);

        List<Map<String, Object>> items = jdbc.queryForList(sql, args.toArray());

        String totalSql = "SELECT COUNT(DISTINCT a.id) FROM artwork a WHERE " + String.join(" AND ", where);
        // Category relation join is deliberately not included in the total: total means unique artworks.
        Long total = jdbc.queryForObject(totalSql, Long.class, args.toArray());

        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> row : items) {
            Map<String, Object> item = new LinkedHashMap<>();
            for (int i = 0; i < dimensions.size(); i++) {
                String key = dimensions.get(i);
                item.put(key + "Id", row.get("dim" + i + "Id"));
                item.put(key + "Label", row.get("dim" + i + "Label"));
            }
            long count = ((Number) row.get("count")).longValue();
            item.put("count", count);
            item.put("percentage", total == null || total == 0 ? 0.0
                    : Math.round(count * 10000.0 / total) / 100.0);
            normalized.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("groupBy", dimensions);
        response.put("categoryScope", dimensions.contains("category") ? scope : null);
        response.put("total", total == null ? 0L : total);
        response.put("items", normalized);
        return response;
    }

    public List<Map<String, Object>> approvalStatistics(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from 不能晚于 to");
        }

        StringBuilder sql = new StringBuilder("""
                SELECT business_type AS businessType,
                       COUNT(*) AS total,
                       SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending,
                       SUM(CASE WHEN status IN ('APPROVED', 'COMPLETED') THEN 1 ELSE 0 END) AS approved,
                       SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected,
                       SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed
                FROM approval_request
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (from != null) {
            sql.append(" AND created_at >= ?");
            args.add(from.atStartOfDay());
        }
        if (to != null) {
            sql.append(" AND created_at < ?");
            args.add(to.plusDays(1).atStartOfDay());
        }
        sql.append(" GROUP BY business_type ORDER BY business_type");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
        for (Map<String, Object> row : rows) {
            long approved = ((Number) row.get("approved")).longValue();
            long rejected = ((Number) row.get("rejected")).longValue();
            long decided = approved + rejected;
            row.put("passRate", decided == 0 ? 0.0
                    : Math.round(approved * 10000.0 / decided) / 100.0);
        }
        return rows;
    }

    private List<String> parseDimensions(String groupBy) {
        if (groupBy == null || groupBy.isBlank()) {
            throw new IllegalArgumentException("groupBy 必填，例如 status 或 status,condition");
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String raw : groupBy.split(",")) {
            String key = raw.trim().toLowerCase(Locale.ROOT);
            if (!DIMENSIONS.containsKey(key)) {
                throw new IllegalArgumentException("不支持的统计维度: " + key
                        + "；支持 category、condition、status、location");
            }
            result.add(key);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("至少指定一个统计维度");
        }
        if (result.size() > 3) {
            throw new IllegalArgumentException("一次最多支持三个统计维度");
        }
        return new ArrayList<>(result);
    }

    private Long one(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }
}
