package com.hml.museum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

/** RustFS S3 存储适配器。 */
@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${museum.storage.bucket}")
    private String bucket;

    @Value("${museum.storage.presign-minutes:30}")
    private long presignMinutes;

    @Value("${museum.storage.upload-expiry-minutes:60}")
    private long uploadExpiryMinutes;

    public String createUploadUrl(String objectKey) {
        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(uploadExpiryMinutes))
                    .putObjectRequest(objectRequest)
                    .build();

            return presigner.presignPutObject(presignRequest).url().toString();
        } catch (Exception e) {
            throw new IllegalStateException("生成 RustFS 上传地址失败", e);
        }
    }

    public String createDownloadUrl(String objectKey) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(presignMinutes))
                    .getObjectRequest(objectRequest)
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            throw new IllegalStateException("生成 RustFS 下载地址失败", e);
        }
    }

    public void putBytes(String objectKey, InputStream in, long size, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength(size)
                    .build();
            s3.putObject(request, RequestBody.fromInputStream(in, size));
        } catch (Exception e) {
            throw new IllegalStateException("上传 RustFS 对象失败", e);
        }
    }

    /** 验证浏览器是否已经真正把对象上传到 RustFS。 */
    public HeadObjectResponse head(String objectKey) {
        try {
            return s3.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("读取 RustFS 对象元数据失败", e);
        }
    }

    public void delete(String objectKey) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("删除 RustFS 对象失败", e);
        }
    }

    public InputStream get(String objectKey) {
        try {
            return s3.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .build(),
                    ResponseTransformer.toInputStream());
        } catch (Exception e) {
            throw new IllegalStateException("读取 RustFS 对象失败", e);
        }
    }

    public List<S3Object> list(String prefix) {
        return s3.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(prefix == null ? "" : prefix)
                        .build())
                .contents();
    }
}
