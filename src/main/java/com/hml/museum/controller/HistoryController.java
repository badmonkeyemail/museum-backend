package com.hml.museum.controller;

import com.hml.museum.entity.ArtworkHistory;
import com.hml.museum.service.ArtworkHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artworks/{artworkId}/history")
@RequiredArgsConstructor
public class HistoryController {
    private final ArtworkHistoryService service;

    @GetMapping
    public List<ArtworkHistory> list(@PathVariable Long artworkId) {
        return service.list(artworkId);
    }
}
