package com.hml.museum.repository;

import com.hml.museum.entity.Artwork;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ArtworkRepository
        extends JpaRepository<Artwork, Long>, JpaSpecificationExecutor<Artwork> {

    boolean existsByRegistrationNoAndDeleted(String registrationNo, Integer deleted);

    Optional<Artwork> findByIdAndDeleted(Long id, Integer deleted);

    Page<Artwork> findByDeleted(Integer deleted, Pageable pageable);

    /**
     * 设置封面时锁定作品行。
     * 同一作品的并发“设置封面”操作会在事务层串行化，保证最多一个 is_primary=1。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a
            from Artwork a
            where a.id = :id
              and a.deleted = :deleted
            """)
    Optional<Artwork> findByIdAndDeletedForUpdate(
            @Param("id") Long id,
            @Param("deleted") Integer deleted
    );

    @Query(value = """
            SELECT *
            FROM artwork
            WHERE deleted = 0
              AND MATCH(fulltext_content)
                  AGAINST(:keyword IN NATURAL LANGUAGE MODE)
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM artwork
            WHERE deleted = 0
              AND MATCH(fulltext_content)
                  AGAINST(:keyword IN NATURAL LANGUAGE MODE)
            """,
            nativeQuery = true)
    Page<Artwork> fullTextSearch(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
