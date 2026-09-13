package com.clipzy.service;

import com.clipzy.config.ClipzyProperties;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class StorageService {

  private final S3Client s3Client;
  private final ClipzyProperties properties;

  public StorageService(S3Client s3Client, ClipzyProperties properties) {
    this.s3Client = s3Client;
    this.properties = properties;
  }

  public void download(String key, Path destination) {
    s3Client.getObject(
        GetObjectRequest.builder()
            .bucket(properties.getS3().getBucket())
            .key(key)
            .build(),
        ResponseTransformer.toFile(destination)
    );
  }

  public void uploadFile(String key, Path file, String contentType) {
    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(properties.getS3().getBucket())
            .key(key)
            .contentType(contentType)
            .build(),
        RequestBody.fromFile(file)
    );
  }

  public String bucket() {
    return properties.getS3().getBucket();
  }
}
