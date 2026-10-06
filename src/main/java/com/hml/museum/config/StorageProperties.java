package com.hml.museum.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "museum.storage")
public class StorageProperties {
    private String endpoint = "http://127.0.0.1:19000";
    private String accessKey = "rustfsadmin";
    private String secretKey = "rustfsadmin";
    private String region = "us-east-1";
    private String bucket = "museum-images";
    private int presignMinutes = 30;
    private int uploadExpiryMinutes = 60;

}