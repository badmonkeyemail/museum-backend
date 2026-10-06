package com.hml.museum.controller;

import com.hml.museum.dto.ArtworkDtos;
import com.hml.museum.result.Result;
import com.hml.museum.result.ResultCode;
import com.hml.museum.service.ArtworkService;
import com.hml.museum.service.ArtworkSearchService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/artworks")
@RequiredArgsConstructor
public class ArtworkController {
    private final ArtworkService service;
    private final ArtworkSearchService searchService;

    @GetMapping("/{id}")
    public Result<ArtworkDtos.Response> get(@PathVariable Long id) {
        return Result.success(ResultCode.SUCCESS, service.get(id));
    }

    @GetMapping
    public Result<Page<ArtworkDtos.ListResponse>> page(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        int actualSize = Math.min(Math.max(size, 1), 200);
        return Result.success(ResultCode.SUCCESS, service.page(PageRequest.of(page, actualSize, Sort.by("id").descending())));
    }

    @GetMapping("/full-text-search")
    public Result<Page<ArtworkDtos.ListResponse>> fullTextSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(ResultCode.SUCCESS, searchService.fullTextSearch(keyword, page, size));
    }

    @PostMapping
    public Result<ArtworkDtos.Response> create(@Valid @RequestBody ArtworkDtos.SaveRequest r) {
        return Result.success(ResultCode.SUCCESS_INSERT, service.create(r));
    }

    @PutMapping("/{id}")
    public Result<ArtworkDtos.Response> update(@PathVariable Long id, @Valid @RequestBody ArtworkDtos.SaveRequest r) {
        return Result.success(ResultCode.SUCCESS_UPDATE, service.update(id, r));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Integer operatorId, @RequestParam(required = false) String ipAddress) {
        service.delete(id, operatorId, ipAddress);
        return Result.success(ResultCode.SUCCESS_DELETE);
    }
}
