package com.hml.museum.repository;

import com.hml.museum.entity.Multimedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MultimediaRepository extends JpaRepository<Multimedia, Long> {

    List<Multimedia> findByArtworkIdAndDeletedAndStatusOrderBySortOrderAscIdAsc(
            Long artworkId,
            Integer deleted,
            Integer status
    );

    List<Multimedia> findByArtworkIdAndDeletedOrderBySortOrderAscIdAsc(
            Long artworkId,
            Integer deleted
    );

    List<Multimedia> findByArtworkIdInAndDeletedAndStatus(
            Collection<Long> artworkIds,
            Integer deleted,
            Integer status
    );

    Optional<Multimedia> findByIdAndDeleted(Long id, Integer deleted);

    Optional<Multimedia> findByIdAndArtworkIdAndDeleted(
            Long id,
            Long artworkId,
            Integer deleted
    );

    long countByArtworkIdAndDeletedAndStatus(
            Long artworkId,
            Integer deleted,
            Integer status
    );

    Optional<Multimedia>
    findByArtworkIdAndPrimaryAndDeleted(
            Long artworkId,
            Integer primary,
            Integer deleted
    );

    /** 查询指定作品当前的主照片。 */
    Optional<Multimedia>
    findFirstByArtworkIdAndPrimaryAndMultimediaTypeIdAndDeletedAndStatusOrderBySortOrderAscIdAsc(
            Long artworkId,
            Integer primary,
            Integer multimediaTypeId,
            Integer deleted,
            Integer status
    );

    /** 批量查询作品的主照片，用于作品列表，避免 N+1 查询。 */
    List<Multimedia>
    findByArtworkIdInAndPrimaryAndMultimediaTypeIdAndDeletedAndStatusOrderBySortOrderAscIdAsc(
            Collection<Long> artworkIds,
            Integer primary,
            Integer multimediaTypeId,
            Integer deleted,
            Integer status
    );

    List<Multimedia>
    findByArtworkIdInAndPrimaryAndDeleted(
            Collection<Long> artworkIds,
            Integer primary,
            Integer deleted
    );

    /** 在已经锁定 artwork 行以后，将该作品其他媒体的封面标志全部清零。 */
    @Modifying
    @Query("""
            update Multimedia m
               set m.primary = 0
             where m.artworkId = :artworkId
            """)
    int clearPrimaryByArtworkId(
            @Param("artworkId") Long artworkId
    );

    /** 删除作品时，一并查询其有效媒体，供级联逻辑删除。 */
    List<Multimedia> findByArtworkIdAndDeleted(
            Long artworkId,
            Integer deleted
    );
}
