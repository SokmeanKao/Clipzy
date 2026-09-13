package com.clipzy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "watch_history")
public class WatchHistory {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "video_id", nullable = false)
  private Video video;

  @Column(name = "progress_seconds", nullable = false)
  private int progressSeconds;

  @Column(name = "last_watched_at", nullable = false)
  private Instant lastWatchedAt;

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (lastWatchedAt == null) {
      lastWatchedAt = Instant.now();
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Video getVideo() {
    return video;
  }

  public void setVideo(Video video) {
    this.video = video;
  }

  public int getProgressSeconds() {
    return progressSeconds;
  }

  public void setProgressSeconds(int progressSeconds) {
    this.progressSeconds = progressSeconds;
  }

  public Instant getLastWatchedAt() {
    return lastWatchedAt;
  }

  public void setLastWatchedAt(Instant lastWatchedAt) {
    this.lastWatchedAt = lastWatchedAt;
  }
}
