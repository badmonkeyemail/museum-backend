package com.hml.museum.controller;

import com.hml.museum.dto.ArtworkConditionDtos;
import com.hml.museum.dto.ArtworkStatusDtos;
import com.hml.museum.result.Result;
import com.hml.museum.result.ResultCode;
import com.hml.museum.service.ArtworkStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artwork-status")
@RequiredArgsConstructor
public class ArtworkStatusController {

    private final ArtworkStatusService service;

    /** 查询全部 */
    @GetMapping
    public Result<List<ArtworkStatusDtos.Response>> list() {
        return Result.success(ResultCode.SUCCESS, service.list());
    }

    @GetMapping("/valid-list")
    public Result<List<ArtworkStatusDtos.Response>> validList() {
        return Result.success(ResultCode.SUCCESS, service.validList());
    }

    /** 查询单个 */
    @GetMapping("/{id}")
    public Result<ArtworkStatusDtos.Response> getById(@PathVariable Integer id) {
        return Result.success(ResultCode.SUCCESS, service.getById(id));
    }

    /** 添加 */
    @PostMapping
    //@ResponseStatus(HttpStatus.CREATED)
    public Result<ArtworkStatusDtos.Response> create(@Valid @RequestBody ArtworkStatusDtos.CreateRequest req) {
        return Result.success(ResultCode.SUCCESS_INSERT, service.create(req));
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<ArtworkStatusDtos.Response> update(@PathVariable Integer id,
                                                        @Valid @RequestBody ArtworkStatusDtos.UpdateRequest req) {
        return Result.success(ResultCode.SUCCESS_UPDATE, service.update(id, req));
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    //@ResponseStatus(HttpStatus.NO_CONTENT)
    public Result<Void> delete(@PathVariable Integer id) {
        service.delete(id); //异常处理会在service中定义，并统一由BusinessException处理

        // 只要上面不抛异常，程序执行到这里直接返回删除成功的统一结构
        return Result.success(ResultCode.SUCCESS_DELETE);
    }

}
