package com.hml.museum.controller;

import com.hml.museum.dto.ArtworkDtos;
import com.hml.museum.dto.ArtworkSearchDtos;
import com.hml.museum.result.Result;
import com.hml.museum.result.ResultCode;
import com.hml.museum.service.ArtworkSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artworks/search")
@RequiredArgsConstructor
public class ArtworkSearchController {

    private final ArtworkSearchService searchService;

    /** GET /api/artworks/search/fields */
    @GetMapping("/fields")
    public Result<List<ArtworkSearchDtos.FieldMetadata>> fields() {
        return Result.success(
                ResultCode.SUCCESS,
                searchService.getFieldMetadata()
        );
    }

    /** POST /api/artworks/search：最多两个条件，条件之间固定 AND。 */
    @PostMapping
    public Result<Page<ArtworkDtos.ListResponse>> search(
            @Valid @RequestBody ArtworkSearchDtos.SearchRequest request
    ) {
        return Result.success(
                ResultCode.SUCCESS,
                searchService.search(request)
        );
    }

    /** GET /api/artworks/search/full-text?keyword=... */
    @GetMapping("/full-text")
    public Result<Page<ArtworkDtos.ListResponse>> fullTextSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return Result.success(
                ResultCode.SUCCESS,
                searchService.fullTextSearch(keyword, page, size)
        );
    }
}
