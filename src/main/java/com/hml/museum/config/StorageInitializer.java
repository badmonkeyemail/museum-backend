package com.hml.museum.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

/**
 * 启动时确保 RustFS 的 S3 bucket 存在。
 */
@Component
@RequiredArgsConstructor
public class StorageInitializer implements CommandLineRunner {

    private final S3Client s3;

    @Value("${museum.storage.bucket}")
    private String bucket;

    @Override
    public void run(String... args) {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (Exception ignored) {
            try {
                s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ignoredAgain) {
                // 并发启动时可能已经由另一个实例创建成功。
            } catch (Exception e) {
                throw new IllegalStateException("初始化 RustFS bucket 失败: " + bucket, e);
            }
        }
    }
}
