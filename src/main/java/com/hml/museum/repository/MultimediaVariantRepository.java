package com.hml.museum.repository;

import com.hml.museum.entity.MultimediaVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MultimediaVariantRepository extends JpaRepository<MultimediaVariant, Long> {

    List<MultimediaVariant> findByMultimediaIdAndDeletedOrderByVariantTypeAsc(
            Long multimediaId,
            Integer deleted
    );

    Optional<MultimediaVariant> findByMultimediaIdAndVariantTypeAndDeleted(
            Long multimediaId,
            String variantType,
            Integer deleted
    );

    /** 查询所有状态的同一变体，用于逻辑删除后重新生成时复用原记录。 */
    Optional<MultimediaVariant> findByMultimediaIdAndVariantType(
            Long multimediaId,
            String variantType
    );
}
