package com.clipzy.web;

import com.clipzy.dto.ChannelResponse;
import com.clipzy.security.UserPrincipal;
import com.clipzy.service.ChannelService;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/channels")
public class ChannelController {

  private final ChannelService channelService;

  public ChannelController(ChannelService channelService) {
    this.channelService = channelService;
  }

  @GetMapping("/{userId}")
  public ChannelResponse get(
      @PathVariable UUID userId,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    return channelService.getChannel(userId, pageable);
  }

  @PostMapping("/{id}/subscribe")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, String> subscribe(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable("id") UUID channelId
  ) {
    channelService.subscribe(principal, channelId);
    return Map.of("status", "subscribed");
  }

  @DeleteMapping("/{id}/subscribe")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unsubscribe(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable("id") UUID channelId
  ) {
    channelService.unsubscribe(principal, channelId);
  }
}
