package com.hml.museum.controller;

import com.hml.museum.entity.MultimediaVariant;
import com.hml.museum.repository.MultimediaVariantRepository;
import com.hml.museum.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {
    private final MultimediaVariantRepository variants;
    private final ImageStorageService storage;

    @GetMapping("/variants/{variantId}/url")
    public Map<String, String> url(@PathVariable Long variantId) {
        MultimediaVariant v = variants.findById(variantId).orElseThrow();
        // 原图/高保真图不直接暴露 URL；后续接审批授权后在 DownloadService 中生成短期 URL。
        if (!v.getVariantType().startsWith("THUMB_"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "高清图片/原图需要审批授权");
        return Map.of("url", storage.createDownloadUrl(v.getObjectKey()));
    }
}
