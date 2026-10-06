package com.hml.museum.repository;

import com.hml.museum.entity.ArtworkCategoryRelation;
import com.hml.museum.entity.ArtworkCategoryRelationKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtworkCategoryRelationRepository extends JpaRepository<ArtworkCategoryRelation, ArtworkCategoryRelationKey> {
    List<ArtworkCategoryRelation> findByIdArtworkId(Long artworkId);

    long countByIdCategoryId(Integer categoryId);

    void deleteByIdArtworkId(Long artworkId);
}
