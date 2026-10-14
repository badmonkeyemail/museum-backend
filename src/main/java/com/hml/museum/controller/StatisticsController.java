package com.hml.museum.controller;

import com.hml.museum.service.StatisticsService;
import com.hml.museum.service.StorageStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService service;
    private final StorageStatisticsService storageStatisticsService;

    /** 藏品总览，保留旧接口以兼容现有前端。 */
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return service.overview();
    }

    /** 分类统计旧接口：按所有关联分类计数。 */
    @GetMapping("/categories")
    public List<Map<String, Object>> categories() {
        return service.byCategory();
    }

    /**
     * 统一藏品统计接口。
     * groupBy 支持 category、condition、status、location，可组合最多三个维度。
     * categoryScope=primary 表示只按主分类统计；all 表示按全部关联分类统计。
     */
    @GetMapping("/artworks")
    public Map<String, Object> artworks(
            @RequestParam String groupBy,
            @RequestParam(required = false, defaultValue = "primary") String categoryScope,
            @RequestParam(required = false) Integer conditionId,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(required = false) Integer locationCategoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.artworks(groupBy, categoryScope, conditionId, statusId,
                locationCategoryId, from, to);
    }

    /** 按审批业务类型统计；from/to 按申请创建时间过滤，日期包含首尾两天。 */
    @GetMapping("/approvals")
    public List<Map<String, Object>> approvals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.approvalStatistics(from, to);
    }

    /** 数据库、RustFS 对象清单和当前应用可访问磁盘的容量信息。 */
    @GetMapping("/storage")
    public Map<String, Object> storage() {
        return storageStatisticsService.snapshot();
    }
}
