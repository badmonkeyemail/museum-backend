package com.hml.museum.controller;

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

    @GetMapping("/tree")
    public Result<List<LocationCategoryDtos.CategoryTree>> tree() {
        return Result.success(ResultCode.SUCCESS, service.tree());
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
