package com.clipzy.repository;

import com.clipzy.domain.WatchHistory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, UUID> {

  Optional<WatchHistory> findByUserIdAndVideoId(UUID userId, UUID videoId);

  Page<WatchHistory> findByUserIdOrderByLastWatchedAtDesc(UUID userId, Pageable pageable);
}
