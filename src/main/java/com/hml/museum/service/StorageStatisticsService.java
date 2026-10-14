package com.hml.museum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.FileStore;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 存储空间快照。
 *
 * 注意：disk 仅代表 museum.statistics.storage-path 所在文件系统，
 * 不代表独立 RustFS 主机或容器的物理磁盘。
 */
@Service
@RequiredArgsConstructor
public class StorageStatisticsService {
    private final JdbcTemplate jdbc;
    private final S3Client s3;

    @Value("${museum.storage.bucket}")
    private String bucket;

    @Value("${museum.statistics.storage-path:${user.dir}}")
    private String storagePath;

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("collectedAt", java.time.OffsetDateTime.now().toString());
        result.put("mysql", mysqlStats());
        result.put("mediaMetadata", mediaMetadataStats());
        result.put("rustfs", rustFsStats());
        result.put("disk", diskStats());
        return result;
    }

    private Map<String, Object> mysqlStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        String database = jdbc.queryForObject("SELECT DATABASE()", String.class);
        Map<String, Object> sizes = jdbc.queryForMap("""
                SELECT COALESCE(SUM(data_length), 0) AS dataBytes,
                       COALESCE(SUM(index_length), 0) AS indexBytes,
                       COALESCE(SUM(data_length + index_length), 0) AS totalBytes,
                       COUNT(*) AS tableCount
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                """);
        result.put("database", database);
        result.putAll(sizes);
        result.put("totalBytesHuman", humanBytes(number(sizes.get("totalBytes"))));
        return result;
    }

    private Map<String, Object> mediaMetadataStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM multimedia_variant v
                JOIN multimedia m ON m.id = v.multimedia_id
                WHERE v.deleted = 0 AND v.status = 1
                  AND m.deleted = 0 AND m.status = 1
                """, Long.class);
        Long bytes = jdbc.queryForObject("""
                SELECT COALESCE(SUM(v.file_size), 0)
                FROM multimedia_variant v
                JOIN multimedia m ON m.id = v.multimedia_id
                WHERE v.deleted = 0 AND v.status = 1
                  AND m.deleted = 0 AND m.status = 1
                """, Long.class);
        result.put("activeVariantCount", count == null ? 0L : count);
        result.put("recordedFileBytes", bytes == null ? 0L : bytes);
        result.put("recordedFileBytesHuman", humanBytes(bytes == null ? 0L : bytes));
        return result;
    }

    private Map<String, Object> rustFsStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bucket", bucket);
        try {
            long count = 0L;
            long bytes = 0L;
            var request = ListObjectsV2Request.builder().bucket(bucket).build();
            for (var page : s3.listObjectsV2Paginator(request)) {
                for (var object : page.contents()) {
                    count++;
                    if (object.size() != null) {
                        bytes += object.size();
                    }
                }
            }
            result.put("available", true);
            result.put("objectCount", count);
            result.put("objectBytes", bytes);
            result.put("objectBytesHuman", humanBytes(bytes));
        } catch (Exception ex) {
            // RustFS 暂时不可用时，仍返回 MySQL 和本地磁盘统计。
            result.put("available", false);
            result.put("objectCount", null);
            result.put("objectBytes", null);
            result.put("error", "无法读取 RustFS 对象清单，请检查 RustFS 连接和桶权限");
        }
        return result;
    }

    private Map<String, Object> diskStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        Path path = Paths.get(storagePath).toAbsolutePath().normalize();
        result.put("path", path.toString());
        try {
            FileStore store = Files.getFileStore(path);
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            long unallocated = store.getUnallocatedSpace();
            result.put("available", true);
            result.put("totalBytes", total);
            result.put("usedBytes", Math.max(0L, total - unallocated));
            result.put("freeBytes", unallocated);
            result.put("usableBytes", usable);
            result.put("totalBytesHuman", humanBytes(total));
            result.put("usedBytesHuman", humanBytes(Math.max(0L, total - unallocated)));
            result.put("usableBytesHuman", humanBytes(usable));
            result.put("fileStore", store.toString());
        } catch (IOException | RuntimeException ex) {
            result.put("available", false);
            result.put("error", "无法读取配置路径所在的文件系统，请确认路径存在且应用有访问权限");
        }
        return result;
    }

    private long number(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double value = bytes;
        String[] units = {"B", "KiB", "MiB", "GiB", "TiB", "PiB"};
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024.0;
            unit++;
        }
        return String.format(java.util.Locale.ROOT, "%.2f %s", value, units[unit]);
    }
}
