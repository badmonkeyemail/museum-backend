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

The current approval entity stores `applicantId`, not an applicant display name, so the form deliberately displays the ID.

The frontend's `requestData` is a JSON string. The current field mapping includes this payload shape:

```json
{
  "purpose": "临时展览",
  "reason": "在法国举办的绘画展览",
  "back_time": "2026-12-10"
}
```

- `purpose` -> PDF purpose
- `reason` -> PDF reason
- `back_time` -> PDF expected return time

The service also retains camelCase and legacy aliases for compatibility. If the frontend changes these JSON property names, update the aliases in `ApprovalPdfService.buildModel`.


## 业务类型对应的附件页

- `OUTBOUND`、`REPRODUCTION`：继续使用 `审批表.html` 中的作品信息附件页。
- `HIGH_RES_DOWNLOAD`：第一页仍使用通用审批表；第二页由独立模板 `高清图下载审批.html` 生成。每个申请明细只展示对应 `THUMB_256` 缩略图和 `multimedia_variant.file_name`，不展示作品档案字段或 RustFS 对象地址。
- 高清图明细必须传入同一作品的 `multimediaId` 和对应 `HIGH_RES` 的 `variantId`；后端校验作品、多媒体及变体归属。

## 高清图下载 Grant / Token 流程

1. 管理员审批通过 `HIGH_RES_DOWNLOAD` 后，后端创建一条 `approval_download_grant`，生成 256-bit 随机 Token。数据库只保存 Token 的 SHA-256 哈希，不保存明文 Token。
2. 审批决定接口响应中的 `downloadToken` 和 `downloadExpiresAt` 是明文 Token 唯一返回时机；请前端安全保存并展示给获准申请人。普通审批查询不会再次返回明文 Token。
3. 下载接口：`GET /api/approvals/download/{token}`。后端校验 Token 哈希、授权状态、有效期和审批类型，再为每个获准的 HIGH_RES 文件生成 RustFS 短时效预签名 URL，并返回文件名及 URL。
4. Token 在有效期内允许重复请求；每次签发下载 URL 都累计 `download_count` 并更新 `last_downloaded_at`。RustFS URL 本身仍受 `museum.storage.presign-minutes` 控制，不能把它当作 Grant Token。
5. 默认 Token 有效期为 24 小时，可通过环境变量 `MUSEUM_APPROVAL_DOWNLOAD_TOKEN_HOURS` 配置。Token 过期后必须重新发起审批。

数据库迁移：`V20261103_01__approval_download_grant.sql`。

## 审批通过后的业务动作

- `OUTBOUND` 和 `REPRODUCTION`：管理员上传签章 PDF 并审批通过后，后端自动调用 `complete()`，按每条明细 `itemData.targetStatusId` 更新作品 `artwork.status_id`，审批状态变为 `COMPLETED`。
- `HIGH_RES_DOWNLOAD`：审批通过只创建下载 Grant，不调用 `complete()`，也不修改作品状态。
