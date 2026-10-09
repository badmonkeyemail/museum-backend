package com.hml.museum.service;

import com.hml.museum.entity.ArtworkHistory;
import com.hml.museum.entity.Multimedia;
import com.hml.museum.entity.MultimediaVariant;
import com.hml.museum.repository.ArtworkHistoryRepository;
import com.hml.museum.repository.MultimediaRepository;
import com.hml.museum.repository.MultimediaVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * 从 HIGH_RES 优先生成三个缩略图。
 * 正常上传由 complete 接口触发异步生成；同步 generate 方法保留给管理员重试/修复接口。
 */
@Service
@RequiredArgsConstructor
public class MultimediaThumbnailService {

    private static final int ACTIVE = 1;
    private static final int NOT_DELETED = 0;

    private final MultimediaVariantRepository repo;
    private final MultimediaRepository multimediaRepo;
    private final ImageStorageService storage;
    private final ArtworkHistoryRepository historyRepo;

    /**
     * 上传完成后由后台异步触发。前端不需要调用此方法。
     */
    @Async("thumbnailTaskExecutor")
    public void generateAsync(Long artworkId, Long multimediaId) {
        generate(artworkId, multimediaId);
    }

    /**
     * 同步生成方法，仅供后台重试/修复接口调用。
     */
    public List<MultimediaVariant> generate(
            Long artworkId,
            Long multimediaId
    ) {
        Multimedia media = multimediaRepo
                .findByIdAndArtworkIdAndDeleted(
                        multimediaId,
                        artworkId,
                        NOT_DELETED
                )
                .orElseThrow(() -> new IllegalStateException("多媒体不存在"));

        if (!Integer.valueOf(MediaTypeIds.PHOTO).equals(media.getMultimediaTypeId())) {
            throw new IllegalArgumentException("只有照片可以生成缩略图");
        }

        MultimediaVariant source = repo
                .findByMultimediaIdAndVariantTypeAndDeletedAndStatus(
                        multimediaId,
                        "HIGH_RES",
                        NOT_DELETED,
                        ACTIVE
                )
                .orElseGet(() -> repo
                        .findByMultimediaIdAndVariantTypeAndDeletedAndStatus(
                                multimediaId,
                                "ORIGINAL",
                                NOT_DELETED,
                                ACTIVE
                        )
                        .orElseThrow(() ->
                                new IllegalStateException("不存在可用于生成缩略图的原图或高保真图")
                        ));

        try (var in = storage.get(source.getObjectKey())) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IllegalStateException("图片无法解析");
            }

            List<MultimediaVariant> result = new ArrayList<>();
            for (int max : new int[]{1024, 256, 64}) {
                BufferedImage out = resize(image, max);

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                boolean written = ImageIO.write(out, "jpg", bytes);
                if (!written) {
                    throw new IllegalStateException("JPEG缩略图编码失败");
                }

                byte[] data = bytes.toByteArray();
                String type = "THUMB_" + max;
                String key = "artwork/"
                        + artworkId
                        + "/multimedia/"
                        + multimediaId
                        + "/"
                        + type.toLowerCase()
                        + ".jpg";

                storage.putBytes(
                        key,
                        new ByteArrayInputStream(data),
                        data.length,
                        "image/jpeg"
                );

                MultimediaVariant variant = repo
                        .findByMultimediaIdAndVariantType(multimediaId, type)
                        .orElseGet(MultimediaVariant::new);

                variant.setMultimediaId(multimediaId);
                variant.setVariantType(type);
                variant.setObjectKey(key);
                variant.setContentType("image/jpeg");
                variant.setFileName(type + ".jpg");
                variant.setFileSize((long) data.length);
                variant.setWidth(out.getWidth());
                variant.setHeight(out.getHeight());
                variant.setChecksumSha256(sha256(data));
                variant.setDeleted(NOT_DELETED);
                variant.setStatus(ACTIVE);

                result.add(repo.save(variant));
            }

            ArtworkHistory history = new ArtworkHistory();
            history.setArtworkId(artworkId);
            history.setOperationType("GENERATE_THUMBNAILS");
            history.setRelatedMultimediaId(multimediaId);
            history.setOperationSummary("生成1024/256/64缩略图");
            historyRepo.save(history);

            return result;
        } catch (IOException e) {
            throw new IllegalStateException("生成缩略图失败", e);
        }
    }

    private BufferedImage resize(BufferedImage src, int max) {
        double scale = Math.min(
                1.0,
                max / (double) Math.max(
                        src.getWidth(),
                        src.getHeight()
                )
        );

        int width = Math.max(
                1,
                (int) Math.round(src.getWidth() * scale)
        );

        int height = Math.max(
                1,
                (int) Math.round(src.getHeight() * scale)
        );

        BufferedImage dst = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );
            g.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );
            g.drawImage(src, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }

        return dst;
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(data);
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256不可用", e);
        }
    }
}
