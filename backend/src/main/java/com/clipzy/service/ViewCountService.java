package com.clipzy.service;

import com.clipzy.repository.VideoRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViewCountService {

  private static final Logger log = LoggerFactory.getLogger(ViewCountService.class);

  private final VideoRepository videoRepository;

  public ViewCountService(VideoRepository videoRepository) {
    this.videoRepository = videoRepository;
  }

  @Async
  @Transactional
  public void incrementAsync(UUID videoId) {
    int updated = videoRepository.incrementViewCount(videoId);
    if (updated == 0) {
      log.debug("View increment skipped; video {} missing", videoId);
    }
  }
}
