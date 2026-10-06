package com.hml.museum.repository;

import com.hml.museum.entity.MultimediaVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MultimediaVariantRepository extends JpaRepository<MultimediaVariant, Long> {

    List<MultimediaVariant> findByMultimediaIdOrderByIdAsc(
            Long multimediaId
    );

    List<MultimediaVariant> findByMultimediaIdAndDeletedOrderByVariantTypeAsc(
            Long multimediaId,
            Integer deleted
    );

    List<MultimediaVariant> findByMultimediaIdAndDeletedAndStatusOrderByVariantTypeAsc(
            Long multimediaId,
            Integer deleted,
            Integer status
    );

    List<MultimediaVariant> findByMultimediaIdInAndDeletedAndStatusOrderByMultimediaIdAscVariantTypeAsc(
            Collection<Long> multimediaIds,
            Integer deleted,
            Integer status
    );

    List<MultimediaVariant> findByMultimediaIdInAndVariantTypeAndDeletedAndStatus(
            Collection<Long> multimediaIds,
            String variantType,
            Integer deleted,
            Integer status
    );

    Optional<MultimediaVariant> findByMultimediaIdAndVariantTypeAndDeleted(
            Long multimediaId,
            String variantType,
            Integer deleted
    );

    Optional<MultimediaVariant> findByMultimediaIdAndVariantTypeAndDeletedAndStatus(
            Long multimediaId,
            String variantType,
            Integer deleted,
            Integer status
    );

    Optional<MultimediaVariant> findByIdAndDeletedAndStatus(
            Long id,
            Integer deleted,
            Integer status
    );

    /** 逻辑删除后重新上传/生成时复用旧记录。 */
    Optional<MultimediaVariant> findByMultimediaIdAndVariantType(
            Long multimediaId,
            String variantType
    );
}
