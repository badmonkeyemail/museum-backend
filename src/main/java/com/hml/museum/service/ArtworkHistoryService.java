package com.hml.museum.service;

import com.hml.museum.entity.ArtworkHistory;
import com.hml.museum.repository.ArtworkHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtworkHistoryService {
    private final ArtworkHistoryRepository repo;

    public List<ArtworkHistory> list(Long artworkId) {
        return repo.findByArtworkIdOrderByOperationAtDesc(artworkId);
    }
}
