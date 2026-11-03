# Approval PDF template

The approval request PDF is rendered from:

`src/main/resources/templates/approval/审批表.html`

The template uses Thymeleaf variables and is converted to PDF by OpenHTMLtoPDF. The approval request and item records remain the source of truth; the HTML is only the presentation layer.

## Chinese font

For reliable Chinese glyph rendering, configure a TrueType/OpenType font file on the host running the backend:

- Windows example: `MUSEUM_PDF_FONT_PATH=C:/Windows/Fonts/simhei.ttf`
- Linux example: `MUSEUM_PDF_FONT_PATH=/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttf`

The path must point to an existing `.ttf` or `.otf` file. The service also checks common system font locations. If no CJK font is found, the PDF can be generated but Chinese text may be missing or replaced.

## Template data

The first page contains approval number, creation time, applicant ID, purpose, item count, reason, expected return time, and blank approval/signature fields. Each approval item produces one attachment page with artwork registration number, name, condition, category path, dimensions, location path, summary, and an optional thumbnail.

The current approval entity stores `applicantId`, not an applicant display name, so the form deliberately displays the ID. Purpose/reason/return-time values are read from matching keys in `request_data`; if the frontend uses different JSON property names, add those names to `ApprovalPdfService.firstText` aliases.
