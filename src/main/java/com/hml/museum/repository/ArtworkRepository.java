package com.hml.museum.repository;

import com.hml.museum.entity.Artwork;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtworkRepository
        extends JpaRepository<Artwork, Long>, JpaSpecificationExecutor<Artwork> {

    boolean existsByRegistrationNoAndDeleted(String registrationNo, Integer deleted);

    java.util.Optional<Artwork> findByIdAndDeleted(Long id, Integer deleted);

    Page<Artwork> findByDeleted(Integer deleted, Pageable pageable);

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
