package com.hml.museum.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "artwork_category_relation")
@Data
@NoArgsConstructor
public class ArtworkCategoryRelation {
    @EmbeddedId
    private ArtworkCategoryRelationKey id;

    @Column(name = "is_primary", nullable = false)
    private Integer primary = 0;
}
