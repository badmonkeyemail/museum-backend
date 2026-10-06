package com.hml.museum.service;

import com.hml.museum.entity.ArtworkHistory;
import com.hml.museum.entity.Multimedia;
import com.hml.museum.entity.MultimediaVariant;
import com.hml.museum.repository.ArtworkHistoryRepository;
import com.hml.museum.repository.MultimediaRepository;
import com.hml.museum.repository.MultimediaVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 从 HIGH_RES 优先生成三个缩略图；实际生产环境建议异步 Worker 执行。
 */
@Service
@RequiredArgsConstructor
public class MultimediaThumbnailService {
    private final MultimediaVariantRepository repo;
    private final MultimediaRepository multimediaRepo;
    private final ImageStorageService storage;
    private final ArtworkHistoryRepository historyRepo;

    public List<MultimediaVariant> generate(Long multimediaId) {
        MultimediaVariant source = repo.findByMultimediaIdAndVariantTypeAndDeleted(multimediaId, "HIGH_RES", 0).orElseGet(() -> repo.findByMultimediaIdAndVariantTypeAndDeleted(multimediaId, "ORIGINAL", 0).orElseThrow(() -> new IllegalStateException("不存在原图或高保真图")));
        Multimedia media = multimediaRepo.findByIdAndDeleted(multimediaId, 0).orElseThrow();
        try (InputStream in = storage.get(source.getObjectKey())) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) throw new IllegalStateException("图片无法解析");
            List<MultimediaVariant> result = new ArrayList<>();
            for (int max : new int[]{1024, 256, 64}) {
                BufferedImage out = resize(image, max);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ImageIO.write(out, "jpg", bytes);
                byte[] data = bytes.toByteArray();
                String type = "THUMB_" + max;
                String key = "artwork/" + media.getArtworkId() + "/multimedia/" + multimediaId + "/" + type.toLowerCase() + ".jpg";
                storage.putBytes(key, new ByteArrayInputStream(data), data.length, "image/jpeg");
                MultimediaVariant v = repo.findByMultimediaIdAndVariantTypeAndDeleted(multimediaId, type, 0).orElse(new MultimediaVariant());
                v.setMultimediaId(multimediaId);
                v.setVariantType(type);
                v.setObjectKey(key);
                v.setContentType("image/jpeg");
                v.setFileName(type + ".jpg");
                v.setFileSize((long) data.length);
                v.setWidth(out.getWidth());
                v.setHeight(out.getHeight());
                v.setDeleted(0);
                v.setStatus(1);
                result.add(repo.save(v));
            }
            ArtworkHistory h = new ArtworkHistory();
            h.setArtworkId(media.getArtworkId());
            h.setOperationType("GENERATE_THUMBNAILS");
            h.setRelatedMultimediaId(multimediaId);
            h.setOperationSummary("生成1024/256/64缩略图");
            historyRepo.save(h);
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("生成缩略图失败", e);
        }
    }

    private BufferedImage resize(BufferedImage src, int max) {
        double scale = Math.min(1.0, max / (double) Math.max(src.getWidth(), src.getHeight()));
        int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return dst;
    }
}
