package com.hml.museum.controller;

import com.hml.museum.dto.ArtworkCategoryDtos;
import com.hml.museum.entity.ArtworkCategory;
import com.hml.museum.result.Result;
import com.hml.museum.result.ResultCode;
import com.hml.museum.service.ArtworkCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artwork-categories")
@RequiredArgsConstructor
public class ArtworkCategoryController {
    private final ArtworkCategoryService service;

    @GetMapping("/tree")
    public Result<List<ArtworkCategoryDtos.CategoryTree>> tree() {
        return Result.success(ResultCode.SUCCESS, service.tree());
    }

    @PostMapping
    public Result<ArtworkCategoryDtos.Response> create(@Valid @RequestBody ArtworkCategoryDtos.CreateRequest r) {
        return Result.success(ResultCode.SUCCESS_INSERT, service.create(r));
    }

    @PutMapping("/{id}")
    public Result<ArtworkCategoryDtos.Response> update(@PathVariable Integer id, @Valid @RequestBody ArtworkCategoryDtos.UpdateRequest r) {
        return Result.success(ResultCode.SUCCESS_UPDATE, service.update(id, r));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        // 只要上面不抛异常，程序执行到这里直接返回删除成功的统一结构
        return Result.success(ResultCode.SUCCESS_DELETE);

    }
}
