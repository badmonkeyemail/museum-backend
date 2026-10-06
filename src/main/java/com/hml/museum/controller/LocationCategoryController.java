package com.hml.museum.controller;

import com.hml.museum.dto.ArtworkCategoryDtos;
import com.hml.museum.dto.LocationCategoryDtos;
import com.hml.museum.result.Result;
import com.hml.museum.result.ResultCode;
import com.hml.museum.service.LocationCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location-categories")
@RequiredArgsConstructor
public class LocationCategoryController {
    private final LocationCategoryService service;

    //返回所有数据，包括status=0的停用数据
    @GetMapping("/tree")
    public Result<List<LocationCategoryDtos.CategoryTree>> tree() {
        return Result.success(ResultCode.SUCCESS, service.tree());
    }

    //仅返回status=1的数据，无停用数据
    @GetMapping("/valid-tree")
    public Result<List<LocationCategoryDtos.CategoryTree>> validTree() {
        return Result.success(ResultCode.SUCCESS, service.validTree());
    }

    /**
     * 根据分类节点 ID 获取完整父级路径。
     *
     * GET /api/artwork-categories/{id}/path
     */
    @GetMapping("/{id}/path")
    public Result<LocationCategoryDtos.CategoryPath> path(@PathVariable Integer id) {
        return Result.success(ResultCode.SUCCESS, service.getPath(id));
    }

    @PostMapping
    public Result<LocationCategoryDtos.Response> create(@Valid @RequestBody LocationCategoryDtos.CreateRequest r) {
        return Result.success(ResultCode.SUCCESS_INSERT, service.create(r));
    }

    @PutMapping("/{id}")
    public Result<LocationCategoryDtos.Response> update(@PathVariable Integer id, @Valid @RequestBody LocationCategoryDtos.UpdateRequest r) {
        return Result.success(ResultCode.SUCCESS_UPDATE, service.update(id, r));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        // 只要上面不抛异常，程序执行到这里直接返回删除成功的统一结构
        return Result.success(ResultCode.SUCCESS_DELETE);

    }
}
