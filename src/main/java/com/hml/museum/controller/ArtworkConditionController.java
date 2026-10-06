package com.hml.museum.controller;


import com.hml.museum.result.Result;
import com.hml.museum.result.ResultCode;
import com.hml.museum.service.ArtworkConditionService;
import com.hml.museum.dto.ArtworkConditionDtos;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artwork-condition")
@RequiredArgsConstructor
public class ArtworkConditionController {
    private final ArtworkConditionService service;

    /** 查询全部
     * 增加了参数 HttpServletResponse response，可以设置无缓存，与之配套的代码如下
     *     response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
     *     response.setHeader(HttpHeaders.PRAGMA, "no-cache");
     *     response.setHeader(HttpHeaders.EXPIRES, "0");
     * */
    @GetMapping
    public Result<List<ArtworkConditionDtos.Response>> list(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader(HttpHeaders.EXPIRES, "0");
        return Result.success(ResultCode.SUCCESS, service.list());
    }

    /** 查询单个 */
    @GetMapping("/{id}")
    public Result<ArtworkConditionDtos.Response> getById(@PathVariable Integer id) {
        return Result.success(ResultCode.SUCCESS, service.getById(id));
    }

    /** 添加 */
    @PostMapping
    //@ResponseStatus(HttpStatus.CREATED)
    public Result<ArtworkConditionDtos.Response> create(@Valid @RequestBody ArtworkConditionDtos.CreateRequest req) {
        return Result.success(ResultCode.SUCCESS_INSERT, service.create(req));
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<ArtworkConditionDtos.Response> update(@PathVariable Integer id,
                                        @Valid @RequestBody ArtworkConditionDtos.UpdateRequest req) {
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
