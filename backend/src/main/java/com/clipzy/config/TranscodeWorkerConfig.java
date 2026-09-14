package com.clipzy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bounded pool for concurrent transcode jobs. Each job runs ffmpeg with
 * {@code -threads 0}, so size to roughly half the cores to avoid oversubscription.
 */
@Configuration
public class TranscodeWorkerConfig {

  public static final String EXECUTOR_BEAN = "transcodeExecutor";

  @Bean(name = EXECUTOR_BEAN)
  ThreadPoolTaskExecutor transcodeExecutor(ClipzyProperties properties) {
    int cores = Runtime.getRuntime().availableProcessors();
    int configured = properties.getTranscode().getWorkerPoolSize();
    int poolSize = configured > 0 ? configured : Math.max(1, cores / 2);

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(poolSize);
    executor.setMaxPoolSize(poolSize);
    // Don't queue: scheduler will re-claim on the next poll when a slot frees.
    executor.setQueueCapacity(0);
    executor.setThreadNamePrefix("transcode-");
    executor.setRejectedExecutionHandler((r, ex) -> {
      throw new java.util.concurrent.RejectedExecutionException(
          "transcode pool saturated (size=" + poolSize + ")");
    });
    executor.initialize();
    return executor;
  }
}
