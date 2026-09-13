package com.clipzy.service;

import com.clipzy.domain.Subscription;
import com.clipzy.domain.User;
import com.clipzy.domain.VideoStatus;
import com.clipzy.domain.VideoVisibility;
import com.clipzy.dto.ChannelResponse;
import com.clipzy.dto.VideoResponse;
import com.clipzy.repository.SubscriptionRepository;
import com.clipzy.repository.UserRepository;
import com.clipzy.repository.VideoRepository;
import com.clipzy.security.UserPrincipal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChannelService {

  private final UserRepository userRepository;
  private final VideoRepository videoRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final VideoService videoService;

  public ChannelService(
      UserRepository userRepository,
      VideoRepository videoRepository,
      SubscriptionRepository subscriptionRepository,
      VideoService videoService
  ) {
    this.userRepository = userRepository;
    this.videoRepository = videoRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.videoService = videoService;
  }

  @Transactional(readOnly = true)
  public ChannelResponse getChannel(UUID userId, Pageable pageable) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found"));
    Page<VideoResponse> videos = videoRepository
        .findByOwnerIdAndStatusAndVisibilityOrderByCreatedAtDesc(
            userId, VideoStatus.READY, VideoVisibility.PUBLIC, pageable)
        .map(videoService::toResponse);
    long subscribers = subscriptionRepository.countByIdChannelId(userId);
    return new ChannelResponse(
        user.getId(),
        user.getDisplayName(),
        user.getAvatarUrl(),
        user.getCreatedAt(),
        subscribers,
        videos
    );
  }

  @Transactional
  public void subscribe(UserPrincipal principal, UUID channelId) {
    if (principal.getId().equals(channelId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot subscribe to yourself");
    }
    User subscriber = userRepository.findById(principal.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    User channel = userRepository.findById(channelId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found"));
    if (subscriptionRepository.existsByIdSubscriberIdAndIdChannelId(subscriber.getId(), channel.getId())) {
      return;
    }
    Subscription sub = new Subscription();
    sub.setSubscriber(subscriber);
    sub.setChannel(channel);
    subscriptionRepository.save(sub);
  }

  @Transactional
  public void unsubscribe(UserPrincipal principal, UUID channelId) {
    subscriptionRepository.deleteByIdSubscriberIdAndIdChannelId(principal.getId(), channelId);
  }
}
