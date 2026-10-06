# Museum Archive - RustFS Final

本版本以 RustFS 作为对象存储，Java 端使用标准 AWS S3 SDK 2.x，不再依赖 MinIO Java SDK。

## 技术栈

- Java 17
- Spring Boot 3.5.5
- Spring Data JPA
- MySQL 8.x
- RustFS（S3 API）
- AWS SDK for Java v2 S3
- Flyway

## 本版本的重要统一

### Artwork

- `create_start_time` / `create_end_time` -> `DATETIME(3)`
- Java -> `LocalDateTime`
- `name` -> `VARCHAR(200)`
- `registration_no` -> `VARCHAR(100)`
- `price` -> `VARCHAR(500)`
- `dimensions` -> `VARCHAR(500)`
- `author` -> `VARCHAR(200)`
- `specific_location` -> `VARCHAR(255)`
- `location_category_id` 可为空

### 分类

`artwork_category` 和 `location_category` 均只使用 `status`，不使用 `deleted`。
删除/停用使用 `status = 0`；父节点存在子节点时禁止删除。

### 查询

只使用 `ArtworkSearchDtos`，不再使用 `ArtworkQueryDtos`。
最多两个条件，条件之间固定 AND。

时间字段支持：

- `EQUALS`
- `BETWEEN`

`BETWEEN` 特殊规则：

- `value1 = null, value2 != null` -> 小于 `value2`
- `value1 != null, value2 = null` -> 大于 `value1`
- 两个都有值 -> 包含两个日期的整个日期范围

Reference/Integer 字段使用 `=`，不会生成 `CAST(... AS CHAR) LIKE ...`。

### fulltext_content

由后端根据以下字段构建：

`name + author + inscription + summary + search_keywords`

数据库对其建立 MySQL FULLTEXT ngram 索引。

## 数据库

- `museum_schema.sql`：全新数据库初始化脚本，包含 169 个作品分类节点。
- `artwork_upgrade_datetime.sql`：已有数据库执行的 artwork 字段升级脚本；Flyway 版本为 `V2__align_artwork_columns.sql`。
- `src/main/resources/db/migration/V1__init_schema.sql`：Flyway 初始化迁移。

## RustFS

默认：

- S3 API：`http://127.0.0.1:19000`
- Console：`http://127.0.0.1:19001`

Spring Boot 配置见 `src/main/resources/application.yml`。

## 打包

```bash
mvn clean package -DskipTests
```

当前交付环境没有 Maven，因此本版本未在生成环境中执行真实 Maven 编译；代码已经进行静态引用、字段、方法和配置检查。
