package com.hml.museum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 一个多媒体对象的实际文件变体：
 * ORIGINAL / HIGH_RES / THUMB_1024 / THUMB_256 / THUMB_64。
 */
@Entity
@Table(name = "multimedia_variant",
        uniqueConstraints = @UniqueConstraint(name = "uk_media_variant", columnNames = {"multimedia_id", "variant_type"}))
@Data
@NoArgsConstructor
public class MultimediaVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "multimedia_id", nullable = false)
    private Long multimediaId;

    @Column(name = "variant_type", nullable = false, length = 40)
    private String variantType;

    @Column(name = "object_key", nullable = false, length = 1000)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

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
