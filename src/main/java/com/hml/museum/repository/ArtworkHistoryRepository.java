package com.hml.museum.repository;

import com.hml.museum.entity.ArtworkHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtworkHistoryRepository extends JpaRepository<ArtworkHistory, Long> {
    List<ArtworkHistory> findByArtworkIdOrderByOperationAtDesc(Long artworkId);
}
