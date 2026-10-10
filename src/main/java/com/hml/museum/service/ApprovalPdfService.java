package com.hml.museum.service;

import com.hml.museum.dto.ApprovalDtos;
import com.hml.museum.entity.ApprovalRequest;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 生成系统审批申请 PDF。
 *
 * PDF 作为审批时的固定申请快照；审批完成后再保存管理员签字/盖章后的 PDF。
 * 当前模板使用 PDFBox 内置字体，因此字段内容使用 ASCII 安全表示。
 * 后续如果需要中文正式印章/字体，应在模板资源中加入 CJK TTF/OTF。
 */
@Service
@RequiredArgsConstructor
public class ApprovalPdfService {

    public byte[] generate(ApprovalRequest request, java.util.List<com.hml.museum.entity.ApprovalItem> items) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(50, 760);
                line(cs, "Museum Approval Form");
                line(cs, "Approval No: " + request.getApprovalNo());
                line(cs, "Business Type: " + request.getBusinessType());
                line(cs, "Applicant ID: " + request.getApplicantId());
                line(cs, "Status: " + request.getStatus());
                line(cs, "Request Data: " + safeAscii(request.getRequestData()));
                line(cs, "Items:");
                for (var item : items) {
                    line(cs, "  Artwork=" + item.getArtworkId()
                            + ", Multimedia=" + item.getMultimediaId()
                            + ", Variant=" + item.getVariantId()
                            + ", Data=" + safeAscii(item.getItemData()));
                }
                line(cs, "Remark: " + safeAscii(request.getRemark()));
                line(cs, "Generated At: " + request.getCreatedAt());
                cs.endText();
            }
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成审批 PDF 失败", e);
        }
    }

    private void line(PDPageContentStream cs, String text) throws java.io.IOException {
        cs.showText(text.length() > 115 ? text.substring(0, 115) : text);
        cs.newLineAtOffset(0, -18);
    }

    private String safeAscii(String value) {
        if (value == null) return "";
        return new String(value.getBytes(StandardCharsets.US_ASCII), StandardCharsets.US_ASCII)
                .replace('?', '_');
    }
}
