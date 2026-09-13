package com.clipzy.web;

import com.clipzy.dto.WatchHistoryResponse;
import com.clipzy.security.UserPrincipal;
import com.clipzy.service.WatchHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HistoryController {

  private final WatchHistoryService watchHistoryService;

  public HistoryController(WatchHistoryService watchHistoryService) {
    this.watchHistoryService = watchHistoryService;
  }

  @GetMapping("/me/history")
  public Page<WatchHistoryResponse> history(
      @AuthenticationPrincipal UserPrincipal principal,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    return watchHistoryService.myHistory(principal, pageable);
  }
}
