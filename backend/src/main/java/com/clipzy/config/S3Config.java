package com.clipzy.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

  @Bean
  S3Client s3Client(ClipzyProperties properties) {
    ClipzyProperties.S3 s3 = properties.getS3();
    return S3Client.builder()
        .endpointOverride(URI.create(s3.getEndpoint()))
        .region(Region.of(s3.getRegion()))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())))
        .serviceConfiguration(S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build())
        .build();
  }

  @Bean
  S3Presigner s3Presigner(ClipzyProperties properties) {
    ClipzyProperties.S3 s3 = properties.getS3();
    return S3Presigner.builder()
        .endpointOverride(URI.create(s3.getEndpoint()))
        .region(Region.of(s3.getRegion()))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())))
        .serviceConfiguration(S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build())
        .build();
  }
}
