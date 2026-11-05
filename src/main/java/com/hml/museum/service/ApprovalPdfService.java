package com.hml.museum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hml.museum.entity.*;
import com.hml.museum.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 生成系统审批申请 PDF。
 * PDF 作为审批时的固定申请快照；审批完成后再保存管理员签字/盖章后的 PDF。
 * 当前模板使用 PDFBox 内置字体，因此字段内容使用 ASCII 安全表示。
 * 后续如果需要中文正式印章/字体，应在模板资源中加入 CJK TTF/OTF。
 */

@Service
@RequiredArgsConstructor
public class ApprovalPdfService {
    private static final Logger log = LoggerFactory.getLogger(ApprovalPdfService.class);
    private static final int NOT_DELETED = 0;
    private static final int ACTIVE = 1;
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");

    private final ArtworkRepository artworks;
    private final ArtworkCategoryRepository categories;
    private final ArtworkConditionRepository conditions;
    private final LocationCategoryRepository locations;
    private final MultimediaRepository multimedia;
    private final MultimediaVariantRepository variants;
    private final ImageStorageService storage;
    private final ObjectMapper mapper;

    @Value("${museum.pdf.font-path:}")
    private String fontPath;

    private final SpringTemplateEngine templateEngine = createTemplateEngine();

    public byte[] generate(ApprovalRequest request, List<ApprovalItem> items) {
        try {
            Context context = new Context(Locale.SIMPLIFIED_CHINESE);
            context.setVariables(buildModel(request, items));

            String html = templateEngine.process("审批表", context);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(html, new ClassPathResource("templates/approval/").getURL().toExternalForm());
                registerCjkFont(builder);
                builder.toStream(out);
                builder.run();
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new IllegalStateException("根据审批表.html模板生成 PDF 失败", e);
        }
    }

    private Map<String, Object> buildModel(ApprovalRequest request, List<ApprovalItem> items) {
        Map<String, Object> model = new HashMap<>();
        JsonNode requestData = parseJson(request.getRequestData());

        model.put("businessType", request.getBusinessType());
        model.put("attachments", List.of());
        model.put("downloadAttachments", List.of());
        model.put("approvalNo", text(request.getApprovalNo()));
        model.put("approvalTime", format(request.getCreatedAt()));
        // 当前审批数据模型只保存 applicantId，故显示 ID，不臆造申请人姓名。
        model.put("applicant", "申请人 ID：" + request.getApplicantId());
        model.put("purpose", firstText(requestData, "purpose", "use", "usage", "applicationPurpose"));
        model.put("artworkSum", items.size() + " 件");
        model.put("reason", firstText(requestData, "reason", "applyReason", "applicationReason", "description"));
        // 前端 requestData 使用 snake_case 的 back_time；保留 camelCase 别名以兼容旧数据。
        model.put("backTime", firstText(requestData, "back_time", "backTime", "returnTime", "expectedReturnTime", "returnDate"));
        // 创建申请时尚未审批，审批意见、审批时间和审批人签章留空供后续审批填写。
        model.put("result", "");
        model.put("completedTime", "");
        model.put("approver", "");
        if ("HIGH_RES_DOWNLOAD".equals(request.getBusinessType())) {
            model.put("downloadAttachments", items.stream().map(this::buildDownloadAttachment).toList());
        } else {
            model.put("attachments", items.stream().map(this::buildAttachment).toList());
        }
        return model;
    }

    private Map<String, Object> buildDownloadAttachment(ApprovalItem item) {
        if (item.getVariantId() == null || item.getMultimediaId() == null) {
            throw new IllegalArgumentException("高清图下载审批明细缺少 multimediaId 或 variantId");
        }
        MultimediaVariant highRes = variants.findByIdAndDeletedAndStatus(item.getVariantId(), NOT_DELETED, ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("高清图文件不存在：" + item.getVariantId()));
        if (!"HIGH_RES".equals(highRes.getVariantType()) || !Objects.equals(highRes.getMultimediaId(), item.getMultimediaId())) {
            throw new IllegalArgumentException("审批明细必须指向对应多媒体对象的 HIGH_RES 变体");
        }
        if (highRes.getFileName() == null || highRes.getFileName().isBlank()) {
            throw new IllegalStateException("高清图变体缺少 file_name，无法生成审批附件");
        }
        MultimediaVariant thumb = variants.findByMultimediaIdAndVariantTypeAndDeletedAndStatus(item.getMultimediaId(), "THUMB_256", NOT_DELETED, ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("高清图审批附件缺少 THUMB_256 缩略图：" + item.getMultimediaId()));
        Map<String, Object> data = new HashMap<>();
        data.put("fileName", highRes.getFileName());
        data.put("imageDataUri", imageDataUri(thumb));
        data.put("artworkId", item.getArtworkId());
        return data;
    }

    private String imageDataUri(MultimediaVariant variant) {
        try (InputStream in = storage.get(variant.getObjectKey())) {
            byte[] bytes = in.readAllBytes();
            String contentType = variant.getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) contentType = "image/jpeg";
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("读取高清图审批附件 THUMB_256 失败：" + variant.getId(), e);
        }
    }
    private Map<String, Object> buildAttachment(ApprovalItem item) {
        Artwork artwork = artworks.findByIdAndDeleted(item.getArtworkId(), NOT_DELETED)
                .orElseThrow(() -> new NoSuchElementException("审批 PDF 对应作品不存在：" + item.getArtworkId()));

        Map<String, Object> data = new HashMap<>();
        data.put("registrationNo", text(artwork.getRegistrationNo()));
        data.put("artworkName", text(artwork.getName()));
        data.put("condition", conditions.findById(artwork.getConditionId())
                .map(ArtworkCondition::getName).orElse(""));
        data.put("category", treePath(artwork.getPrimaryCategoryId(), true));
        data.put("size", text(artwork.getDimensions()));

        String location = treePath(artwork.getLocationCategoryId(), false);
        if (artwork.getSpecificLocation() != null && !artwork.getSpecificLocation().isBlank()) {
            location = location.isBlank() ? artwork.getSpecificLocation()
                    : location + " / " + artwork.getSpecificLocation();
        }
        data.put("location", location);
        data.put("description", text(artwork.getSummary()));
        data.put("imageDataUri", thumbnailDataUri(item, artwork));
        data.put("artworkId", artwork.getId());
        return data;
    }

    private String thumbnailDataUri(ApprovalItem item, Artwork artwork) {
        Optional<Multimedia> media = item.getMultimediaId() == null
                ? multimedia.findFirstByArtworkIdAndPrimaryAndMultimediaTypeIdAndDeletedAndStatusOrderBySortOrderAscIdAsc(
                artwork.getId(), 1, MediaTypeIds.PHOTO, NOT_DELETED, ACTIVE)
                : multimedia.findByIdAndArtworkIdAndDeleted(
                item.getMultimediaId(), artwork.getId(), NOT_DELETED);
        if (media.isEmpty()) return null;

        Optional<MultimediaVariant> thumbnail = variants
                .findByMultimediaIdAndVariantTypeAndDeletedAndStatus(
                        media.get().getId(), "THUMB_256", NOT_DELETED, ACTIVE)
                .or(() -> variants.findByMultimediaIdAndVariantTypeAndDeletedAndStatus(
                        media.get().getId(), "THUMB_1024", NOT_DELETED, ACTIVE));
        if (thumbnail.isEmpty()) return null;

        try (InputStream in = storage.get(thumbnail.get().getObjectKey())) {
            byte[] bytes = in.readAllBytes();
            String contentType = thumbnail.get().getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                contentType = "image/jpeg";
            }
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            // 缩略图缺失不应阻断审批申请；附件页仍保留作品文字信息。
            log.warn("读取审批作品缩略图失败，artworkId={}, multimediaId={}",
                    artwork.getId(), media.get().getId(), e);
            return null;
        }
    }

    private String treePath(Integer id, boolean category) {
        if (id == null) return "";
        List<String> names = new ArrayList<>();
        Integer current = id;
        for (int i = 0; current != null && i < 6; i++) {
            if (category) {
                Optional<ArtworkCategory> node = categories.findById(current);
                if (node.isEmpty()) break;
                names.add(node.get().getName());
                current = node.get().getParentId();
            } else {
                Optional<LocationCategory> node = locations.findById(current);
                if (node.isEmpty()) break;
                names.add(node.get().getName());
                current = node.get().getParentId();
            }
        }
        Collections.reverse(names);
        return String.join(" / ", names);
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) return mapper.createObjectNode();
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            return mapper.createObjectNode().put("reason", json);
        }
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (!value.isMissingNode() && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return "";
    }

    private String format(java.time.LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private void registerCjkFont(PdfRendererBuilder builder) {
        Path font = configuredOrSystemFont();
        if (font == null) {
            log.warn("未找到中文 PDF 字体。请通过 MUSEUM_PDF_FONT_PATH 指定 .ttf/.otf 字体文件，否则 PDF 中文可能无法正常显示。");
            return;
        }
        builder.useFont(() -> {
            try {
                return Files.newInputStream(font);
            } catch (IOException e) {
                throw new UncheckedIOException("加载中文 PDF 字体失败: " + font, e);
            }
        }, "MuseumCjk");
    }

    private Path configuredOrSystemFont() {
        if (fontPath != null && !fontPath.isBlank()) {
            Path configured = Path.of(fontPath);
            if (Files.isRegularFile(configured)) return configured;
            throw new IllegalStateException("配置的 PDF 字体文件不存在：" + configured);
        }
        List<String> candidates = List.of(
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/msyh.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttf",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttf"
        );
        return candidates.stream().map(Path::of).filter(Files::isRegularFile).findFirst().orElse(null);
    }

    private static SpringTemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/approval/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}