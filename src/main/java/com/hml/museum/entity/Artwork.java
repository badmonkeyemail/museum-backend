package com.hml.museum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 作品主表：只保存当前有效的作品档案。
 * 历史修改不覆盖历史事实，而是同步写入 artwork_history。
 */
@Entity
@Table(name = "artwork")
@Data
@NoArgsConstructor
public class Artwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "primary_category_id")
    private Integer primaryCategoryId;

    /** 创作开始时间，数据库 DATETIME(3)。 */
    @Column(name = "create_start_time")
    private LocalDateTime creationStartTime;

    /** 创作结束时间，数据库 DATETIME(3)。 */
    @Column(name = "create_end_time")
    private LocalDateTime creationEndTime;

    @Column(name = "condition_id", nullable = false)
    private Integer conditionId;

    @Column(name = "dimensions", length = 500)
    private String dimensions;

    @Column(name = "price", length = 500)
    private String price;

    @Column(name = "author", length = 200)
    private String author;

    @Column(name = "registration_no", length = 100)
    private String registrationNo;

    @Column(name = "inscription", columnDefinition = "TEXT")
    private String inscription;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "status_id", nullable = false)
    private Integer statusId;

    @Column(name = "location_category_id")
    private Integer locationCategoryId;

    @Column(name = "specific_location", length = 255)
    private String specificLocation;

    @Column(name = "search_keywords", length = 500)
    private String searchKeywords;

    /**
     * 由后端根据 name/author/inscription/summary/searchKeywords 自动构建，
     * 不直接接受前端提交。
     */
    @Column(name = "fulltext_content", columnDefinition = "TEXT")
    private String fulltextContent;

    @Version
    private int version;

    /** 作品主表仍使用 deleted：0=正常，1=逻辑删除。 */
    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private static final int DEFAULT_STATUS_ID = 1; // 库存

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (statusId == null) statusId = DEFAULT_STATUS_ID;
        if (deleted == null) deleted = 0;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
