package com.clipzy.worker;

import com.clipzy.config.ClipzyProperties;
import com.clipzy.domain.JobStatus;
import com.clipzy.domain.TranscodeJob;
import com.clipzy.repository.TranscodeJobRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TranscodeWorker {

  private final TranscodeJobRepository jobRepository;
  private final TranscodeJobProcessor processor;
  private final ClipzyProperties properties;

  public TranscodeWorker(
      TranscodeJobRepository jobRepository,
      TranscodeJobProcessor processor,
      ClipzyProperties properties
  ) {
    this.jobRepository = jobRepository;
    this.processor = processor;
    this.properties = properties;
  }

  @Scheduled(fixedDelayString = "${clipzy.transcode.poll-ms:5000}")
  public void poll() {
    List<TranscodeJob> pending = jobRepository.findByStatusOrderByCreatedAtAsc(
        JobStatus.PENDING, PageRequest.of(0, properties.getTranscode().getBatchSize()));
    for (TranscodeJob job : pending) {
      processor.process(job.getId());
    }
  }
}
