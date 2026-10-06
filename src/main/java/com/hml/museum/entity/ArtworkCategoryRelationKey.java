package com.hml.museum.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ArtworkCategoryRelationKey implements Serializable {
    @Column(name = "artwork_id")
    private Long artworkId;

    @Column(name = "category_id")
    private Integer categoryId;
}
