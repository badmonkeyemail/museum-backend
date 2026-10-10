package com.hml.museum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_item")
@Data
@NoArgsConstructor
public class ApprovalItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "approval_id", nullable = false)
    private Long approvalId;

    @Column(name = "artwork_id", nullable = false)
    private Long artworkId;

    @Column(name = "multimedia_id")
    private Long multimediaId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "item_data", columnDefinition = "json")
    private String itemData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
