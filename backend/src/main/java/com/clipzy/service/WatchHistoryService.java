package com.clipzy.service;

import com.clipzy.domain.User;
import com.clipzy.domain.Video;
import com.clipzy.domain.WatchHistory;
import com.clipzy.dto.ProgressRequest;
import com.clipzy.dto.WatchHistoryResponse;
import com.clipzy.repository.UserRepository;
import com.clipzy.repository.VideoRepository;
import com.clipzy.repository.WatchHistoryRepository;
import com.clipzy.security.UserPrincipal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WatchHistoryService {

  private final WatchHistoryRepository watchHistoryRepository;
  private final UserRepository userRepository;
  private final VideoRepository videoRepository;
  private final VideoService videoService;

  public WatchHistoryService(
      WatchHistoryRepository watchHistoryRepository,
      UserRepository userRepository,
      VideoRepository videoRepository,
      VideoService videoService
  ) {
    this.watchHistoryRepository = watchHistoryRepository;
    this.userRepository = userRepository;
    this.videoRepository = videoRepository;
    this.videoService = videoService;
  }

  @Transactional
  public WatchHistoryResponse upsertProgress(
      UserPrincipal principal, UUID videoId, ProgressRequest request) {
    User user = userRepository.findById(principal.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    Video video = videoRepository.findById(videoId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));
    videoService.assertCanView(video, principal);

    WatchHistory history = watchHistoryRepository.findByUserIdAndVideoId(user.getId(), videoId)
        .orElseGet(() -> {
          WatchHistory created = new WatchHistory();
          created.setUser(user);
          created.setVideo(video);
          return created;
        });
    history.setProgressSeconds(request.progressSeconds());
    history.setLastWatchedAt(Instant.now());
    watchHistoryRepository.save(history);
    return toResponse(history);
  }

  @Transactional(readOnly = true)
  public Page<WatchHistoryResponse> myHistory(UserPrincipal principal, Pageable pageable) {
    return watchHistoryRepository
        .findByUserIdOrderByLastWatchedAtDesc(principal.getId(), pageable)
        .map(this::toResponse);
  }

  private WatchHistoryResponse toResponse(WatchHistory history) {
    return new WatchHistoryResponse(
        history.getId(),
        history.getVideo().getId(),
        history.getVideo().getTitle(),
        history.getProgressSeconds(),
        history.getLastWatchedAt()
    );
  }
}
