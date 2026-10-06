package com.hml.museum.controller;

import com.hml.museum.dto.MultimediaDtos;
import com.hml.museum.result.Result;
import com.hml.museum.result.ResultCode;
import com.hml.museum.service.MultimediaService;
import com.hml.museum.service.MultimediaThumbnailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artworks/{artworkId}/multimedia")
@RequiredArgsConstructor
public class MultimediaController {

    private final MultimediaService service;
    private final MultimediaThumbnailService thumbnails;

    /** 浏览某件作品的全部有效多媒体及其文件版本。 */
    @GetMapping
    public Result<List<MultimediaDtos.Response>> list(
            @PathVariable Long artworkId
    ) {
        return Result.success(
                ResultCode.SUCCESS,
                service.list(artworkId)
        );
    }

    /** 创建一条逻辑多媒体记录。 */
    @PostMapping
    public Result<MultimediaDtos.Response> create(
            @PathVariable Long artworkId,
            @Valid @RequestBody MultimediaDtos.CreateRequest request
    ) {
        return Result.success(
                ResultCode.SUCCESS_INSERT,
                service.create(artworkId, request)
        );
    }

    /** 只修改 description，不提供通用 update。 */
    @PatchMapping("/{multimediaId}/description")
    public Result<MultimediaDtos.Response> updateDescription(
            @PathVariable Long artworkId,
            @PathVariable Long multimediaId,
            @Valid @RequestBody MultimediaDtos.UpdateDescriptionRequest request
    ) {
        return Result.success(
                ResultCode.SUCCESS_UPDATE,
                service.updateDescription(artworkId, multimediaId, request)
        );
    }

    /**
     * 设置封面。
     * 前端指定具体 multimediaId；后端只保证同一作品只有一个 isPrimary=1。
     */
    @PutMapping("/{multimediaId}/cover")
    public Result<MultimediaDtos.CoverResponse> setCover(
            @PathVariable Long artworkId,
            @PathVariable Long multimediaId
    ) {
        return Result.success(
                ResultCode.SUCCESS_UPDATE,
                service.setCover(artworkId, multimediaId)
        );
    }

    /** 浏览某一多媒体下的文件版本。 */
    @GetMapping("/{multimediaId}/variants")
    public Result<List<MultimediaDtos.VariantResponse>> variants(
            @PathVariable Long artworkId,
            @PathVariable Long multimediaId
    ) {
        return Result.success(
                ResultCode.SUCCESS,
                service.variants(artworkId, multimediaId)
        );
    }

    /** 获取照片文件的 RustFS 预签名 PUT URL。 */
    @PostMapping("/{multimediaId}/upload-url")
    public Result<MultimediaDtos.UploadUrlResponse> uploadUrl(
            @PathVariable Long artworkId,
            @PathVariable Long multimediaId,
            @Valid @RequestBody MultimediaDtos.UploadUrlRequest request
    ) {
        return Result.success(
                ResultCode.SUCCESS,
                service.createUploadUrl(
                        artworkId,
                        multimediaId,
                        request
                )
        );
    }

    /** 浏览器直接上传 RustFS 成功后通知后端。 */
    @PostMapping("/variants/{variantId}/complete")
    public Result<MultimediaDtos.VariantResponse> complete(
            @PathVariable Long artworkId,
            @PathVariable Long variantId,
            @RequestBody MultimediaDtos.CompleteVariantRequest request
    ) {
        return Result.success(
                ResultCode.SUCCESS_UPDATE,
                service.complete(
                        artworkId,
                        variantId,
                        request
                )
        );
    }

    /** 删除一个 multimedia，同时删除其全部文件版本。 */
    @DeleteMapping("/{multimediaId}")
    public Result<Void> delete(
            @PathVariable Long artworkId,
            @PathVariable Long multimediaId
    ) {
        service.delete(artworkId, multimediaId);
        return Result.success(ResultCode.SUCCESS_DELETE);
    }

    /** 删除一个具体文件版本。 */
    @DeleteMapping("/variants/{variantId}")
    public Result<Void> deleteVariant(
            @PathVariable Long artworkId,
            @PathVariable Long variantId
    ) {
        service.deleteVariant(artworkId, variantId);
        return Result.success(ResultCode.SUCCESS_DELETE);
    }

    /** 从 HIGH_RES 优先生成 1024/256/64 缩略图。 */
    @PostMapping("/{multimediaId}/generate-thumbnails")
    public Result<?> thumbnails(
            @PathVariable Long artworkId,
            @PathVariable Long multimediaId
    ) {
        // artworkId 用于校验 REST 资源归属，thumbnail service 会再次校验数据库关联。
        thumbnails.generate(artworkId, multimediaId);
        return Result.success(
                ResultCode.SUCCESS,
                service.variants(artworkId, multimediaId)
        );
    }
}
