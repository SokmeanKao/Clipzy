package com.clipzy.worker;

import com.clipzy.config.ClipzyProperties;
import com.clipzy.config.TranscodeWorkerConfig;
import com.clipzy.repository.TranscodeJobRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cheap poller: claims pending jobs with SKIP LOCKED and dispatches each to the
 * bounded transcode pool. Does not wait for jobs to finish.
 */
@Component
public class TranscodeWorker {

  private static final Logger log = LoggerFactory.getLogger(TranscodeWorker.class);

  private final TranscodeJobRepository jobRepository;
  private final TranscodeJobProcessor processor;
  private final ClipzyProperties properties;
  private final ThreadPoolTaskExecutor transcodeExecutor;
  private final TransactionTemplate transactionTemplate;

  public TranscodeWorker(
      TranscodeJobRepository jobRepository,
      TranscodeJobProcessor processor,
      ClipzyProperties properties,
      @Qualifier(TranscodeWorkerConfig.EXECUTOR_BEAN) ThreadPoolTaskExecutor transcodeExecutor,
      TransactionTemplate transactionTemplate
  ) {
    this.jobRepository = jobRepository;
    this.processor = processor;
    this.properties = properties;
    this.transcodeExecutor = transcodeExecutor;
    this.transactionTemplate = transactionTemplate;
  }

  @Scheduled(fixedDelayString = "${clipzy.transcode.poll-ms:5000}")
  public void poll() {
    int freeSlots = freePoolSlots();
    if (freeSlots <= 0) {
      return;
    }
    int limit = Math.min(properties.getTranscode().getBatchSize(), freeSlots);
    List<UUID> claimed = transactionTemplate.execute(status -> jobRepository.claimPendingJobs(limit));
    if (claimed == null || claimed.isEmpty()) {
      return;
    }
    log.info("Claimed {} transcode job(s); freeSlotsBeforeClaim≈{}", claimed.size(), freeSlots);
    for (UUID jobId : claimed) {
      try {
        transcodeExecutor.execute(() -> processor.processClaimed(jobId));
      } catch (RejectedExecutionException ex) {
        log.warn("Pool saturated while dispatching {}; releasing claim", jobId);
        transactionTemplate.executeWithoutResult(status -> jobRepository.releaseClaim(jobId));
      }
    }
  }

  private int freePoolSlots() {
    int max = transcodeExecutor.getMaxPoolSize();
    int active = transcodeExecutor.getActiveCount();
    int queued = transcodeExecutor.getThreadPoolExecutor().getQueue().size();
    return Math.max(0, max - active - queued);
  }
}
