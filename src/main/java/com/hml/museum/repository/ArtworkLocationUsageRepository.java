package com.hml.museum.repository;

import com.hml.museum.entity.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;

/** 用于检查位置分类是否被作品使用。Artwork 主键类型为 Long。 */
public interface ArtworkLocationUsageRepository extends JpaRepository<Artwork, Long> {
    long countByLocationCategoryIdAndDeleted(Integer locationCategoryId, Integer deleted);
}
