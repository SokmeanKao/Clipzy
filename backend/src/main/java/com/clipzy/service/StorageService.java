package com.clipzy.service;

import com.clipzy.config.ClipzyProperties;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class StorageService {

  private static final Logger log = LoggerFactory.getLogger(StorageService.class);
  private static final int UPLOAD_ATTEMPTS = 3;

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
    uploadFileWithRetry(key, file, contentType);
  }

  public void uploadFileWithRetry(String key, Path file, String contentType) {
    RuntimeException last = null;
    for (int attempt = 1; attempt <= UPLOAD_ATTEMPTS; attempt++) {
      try {
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(properties.getS3().getBucket())
                .key(key)
                .contentType(contentType)
                .build(),
            RequestBody.fromFile(file)
        );
        return;
      } catch (RuntimeException e) {
        last = e;
        log.warn("S3 upload attempt {}/{} failed for {}: {}", attempt, UPLOAD_ATTEMPTS, key, e.getMessage());
        if (attempt < UPLOAD_ATTEMPTS) {
          try {
            Thread.sleep(200L * attempt);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw e;
          }
        }
      }
    }
    throw last;
  }

  public String bucket() {
    return properties.getS3().getBucket();
  }
}
