package com.clipzy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clipzy")
public class ClipzyProperties {

  private final Jwt jwt = new Jwt();
  private final S3 s3 = new S3();
  private final Ffmpeg ffmpeg = new Ffmpeg();
  private final Transcode transcode = new Transcode();

  public Jwt getJwt() {
    return jwt;
  }

  public S3 getS3() {
    return s3;
  }

  public Ffmpeg getFfmpeg() {
    return ffmpeg;
  }

  public Transcode getTranscode() {
    return transcode;
  }

  public static class Jwt {
    /**
     * HS256 secret. Override with env JWT_SECRET in production.
     */
    private String secret =
        "clipzy-local-dev-jwt-secret-change-me-please-32bytes-minimum!!";
    private long accessTokenMinutes = 30;
    private long refreshTokenDays = 14;

    public String getSecret() {
      return secret;
    }

    public void setSecret(String secret) {
      this.secret = secret;
    }

    public long getAccessTokenMinutes() {
      return accessTokenMinutes;
    }

    public void setAccessTokenMinutes(long accessTokenMinutes) {
      this.accessTokenMinutes = accessTokenMinutes;
    }

    public long getRefreshTokenDays() {
      return refreshTokenDays;
    }

    public void setRefreshTokenDays(long refreshTokenDays) {
      this.refreshTokenDays = refreshTokenDays;
    }
  }

  public static class S3 {
    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucket = "videos";
    private String region = "us-east-1";
    /** Public base URL for playback (MinIO path-style). */
    private String publicBaseUrl = "http://localhost:9000/videos";
    /**
     * Browser-facing S3 API origin (scheme+host[+port]), no bucket path.
     * When blank, derived from {@link #publicBaseUrl} by stripping {@code /}{@link #bucket}.
     */
    private String publicEndpoint = "";
    private long presignPutMinutes = 60;

    public String getEndpoint() {
      return endpoint;
    }

    public void setEndpoint(String endpoint) {
      this.endpoint = endpoint;
    }

    public String getAccessKey() {
      return accessKey;
    }

    public void setAccessKey(String accessKey) {
      this.accessKey = accessKey;
    }

    public String getSecretKey() {
      return secretKey;
    }

    public void setSecretKey(String secretKey) {
      this.secretKey = secretKey;
    }

    public String getBucket() {
      return bucket;
    }

    public void setBucket(String bucket) {
      this.bucket = bucket;
    }

    public String getRegion() {
      return region;
    }

    public void setRegion(String region) {
      this.region = region;
    }

    public String getPublicBaseUrl() {
      return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
      this.publicBaseUrl = publicBaseUrl;
    }

    public String getPublicEndpoint() {
      return publicEndpoint;
    }

    public void setPublicEndpoint(String publicEndpoint) {
      this.publicEndpoint = publicEndpoint;
    }

    /** Origin used when signing browser-facing URLs. */
    public String resolvedPublicEndpoint() {
      if (publicEndpoint != null && !publicEndpoint.isBlank()) {
        return publicEndpoint.replaceAll("/$", "");
      }
      if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
        String base = publicBaseUrl.replaceAll("/$", "");
        String suffix = "/" + bucket;
        if (base.endsWith(suffix)) {
          return base.substring(0, base.length() - suffix.length());
        }
        return base;
      }
      return endpoint.replaceAll("/$", "");
    }

    public long getPresignPutMinutes() {
      return presignPutMinutes;
    }

    public void setPresignPutMinutes(long presignPutMinutes) {
      this.presignPutMinutes = presignPutMinutes;
    }
  }

  public static class Ffmpeg {
    /**
     * Executable path or name on PATH. Set clipzy.ffmpeg.path if FFmpeg is not on PATH.
     */
    private String path = "ffmpeg";

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }
  }

  public static class Transcode {
    private long pollMs = 5000;
    /** Max jobs to claim per poll tick (capped further by free pool slots). */
    private int batchSize = 10;
    /**
     * Concurrent ffmpeg jobs. {@code 0} = auto ({@code max(1, cores/2)}).
     * Each job already uses multiple cores via ffmpeg {@code -threads 0}.
     */
    private int workerPoolSize = 0;
    /** Parallel S3 uploads of HLS segments within a single job. */
    private int uploadConcurrency = 8;

    public long getPollMs() {
      return pollMs;
    }

    public void setPollMs(long pollMs) {
      this.pollMs = pollMs;
    }

    public int getBatchSize() {
      return batchSize;
    }

    public void setBatchSize(int batchSize) {
      this.batchSize = batchSize;
    }

    public int getWorkerPoolSize() {
      return workerPoolSize;
    }

    public void setWorkerPoolSize(int workerPoolSize) {
      this.workerPoolSize = workerPoolSize;
    }

    public int getUploadConcurrency() {
      return uploadConcurrency;
    }

    public void setUploadConcurrency(int uploadConcurrency) {
      this.uploadConcurrency = uploadConcurrency;
    }
  }
}

