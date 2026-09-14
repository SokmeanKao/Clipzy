package com.clipzy.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class S3PublicEndpointTest {

  @Test
  void resolvedPublicEndpoint_prefersExplicitPublicEndpoint() {
    ClipzyProperties.S3 s3 = new ClipzyProperties.S3();
    s3.setPublicEndpoint("https://cdn.example");
    s3.setPublicBaseUrl("https://localhost/videos");
    assertEquals("https://cdn.example", s3.resolvedPublicEndpoint());
  }

  @Test
  void resolvedPublicEndpoint_derivesFromPublicBaseUrl_strippingBucketPath() {
    ClipzyProperties.S3 s3 = new ClipzyProperties.S3();
    s3.setBucket("videos");
    s3.setPublicBaseUrl("https://localhost/videos");
    assertEquals("https://localhost", s3.resolvedPublicEndpoint());
  }

  @Test
  void resolvedPublicEndpoint_fallsBackToInternalEndpoint() {
    ClipzyProperties.S3 s3 = new ClipzyProperties.S3();
    s3.setEndpoint("http://minio:9000");
    s3.setPublicBaseUrl("");
    assertEquals("http://minio:9000", s3.resolvedPublicEndpoint());
  }
}
