package com.hml.museum.controller;

import com.hml.museum.dto.MultimediaDtos.*;
import com.hml.museum.entity.*;
import com.hml.museum.service.*;
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

    @GetMapping
    public List<Multimedia> list(@PathVariable Long artworkId) {
        return service.list(artworkId);
    }

    @PostMapping
    public Multimedia create(@PathVariable Long artworkId, @RequestBody CreateRequest r) {
        return service.create(artworkId, r);
    }

    @GetMapping("/{multimediaId}/variants")
    public List<MultimediaVariant> variants(@PathVariable Long multimediaId) {
        return service.variants(multimediaId);
    }

    @PostMapping("/{multimediaId}/upload-url")
    public UploadUrlResponse uploadUrl(@PathVariable Long multimediaId, @Valid @RequestBody UploadUrlRequest r) {
        return service.createUploadUrl(multimediaId, r);
    }

    @PostMapping("/variants/{variantId}/complete")
    public MultimediaVariant complete(@PathVariable Long variantId, @RequestBody CompleteVariantRequest r) {
        return service.complete(variantId, r);
    }

    @DeleteMapping("/variants/{variantId}")
    public void delete(@PathVariable Long variantId) {
        service.deleteVariant(variantId);
    }

    @PostMapping("/{multimediaId}/generate-thumbnails")
    public List<MultimediaVariant> thumbs(@PathVariable Long multimediaId) {
        return thumbnails.generate(multimediaId);
    }
}
