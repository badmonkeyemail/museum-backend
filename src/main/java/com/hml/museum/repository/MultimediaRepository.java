package com.hml.museum.repository;

import com.hml.museum.entity.Multimedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface MultimediaRepository extends JpaRepository<Multimedia, Long> {
    List<Multimedia> findByArtworkIdAndDeletedOrderBySortOrderAscIdAsc(Long artworkId, Integer deleted);

    Optional<Multimedia> findByIdAndDeleted(Long id, Integer deleted);
}
