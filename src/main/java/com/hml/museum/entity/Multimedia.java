package com.hml.museum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 一个逻辑多媒体对象；PHOTO 的实际文件变体由 MultimediaVariant 保存。
 */
@Entity
@Table(name = "multimedia")
@Data
@NoArgsConstructor
public class Multimedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artwork_id", nullable = false)
    private Long artworkId;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "address", length = 2000)
    private String address;

    @Column(name = "multimedia_type_id", nullable = false)
    private Integer multimediaTypeId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 1000;

    @Column(name = "is_primary", nullable = false)
    private Integer primary = 0;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
