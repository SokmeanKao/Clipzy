package com.clipzy.web;

import com.clipzy.dto.CommentRequest;
import com.clipzy.dto.CommentResponse;
import com.clipzy.dto.CreateVideoRequest;
import com.clipzy.dto.CreateVideoResponse;
import com.clipzy.dto.ProgressRequest;
import com.clipzy.dto.VideoResponse;
import com.clipzy.dto.WatchHistoryResponse;
import com.clipzy.security.UserPrincipal;
import com.clipzy.service.CommentService;
import com.clipzy.service.VideoService;
import com.clipzy.service.WatchHistoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/videos")
public class VideoController {

  private final VideoService videoService;
  private final CommentService commentService;
  private final WatchHistoryService watchHistoryService;

  public VideoController(
      VideoService videoService,
      CommentService commentService,
      WatchHistoryService watchHistoryService
  ) {
    this.videoService = videoService;
    this.commentService = commentService;
    this.watchHistoryService = watchHistoryService;
  }

  @GetMapping
  public Page<VideoResponse> feed(@PageableDefault(size = 20) Pageable pageable) {
    return videoService.publicFeed(pageable);
  }

  @GetMapping("/search")
  public Page<VideoResponse> search(
      @RequestParam("q") String q,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    return videoService.search(q, pageable);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CreateVideoResponse create(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody CreateVideoRequest request
  ) {
    return videoService.create(principal, request);
  }

  @GetMapping("/{id}")
  public VideoResponse get(
      @PathVariable UUID id,
      @AuthenticationPrincipal UserPrincipal principal
  ) {
    return videoService.getById(id, principal);
  }

  @PostMapping("/{id}/complete-upload")
  public VideoResponse completeUpload(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable UUID id
  ) {
    return videoService.completeUpload(principal, id);
  }

  @PostMapping("/{id}/view")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> view(@PathVariable UUID id) {
    videoService.recordView(id);
    return Map.of("status", "accepted");
  }

  @GetMapping("/{id}/comments")
  public List<CommentResponse> listComments(
      @PathVariable UUID id,
      @AuthenticationPrincipal UserPrincipal principal
  ) {
    return commentService.list(id, principal);
  }

  @PostMapping("/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public CommentResponse addComment(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody CommentRequest request
  ) {
    return commentService.create(principal, id, request);
  }

  @PostMapping("/{id}/progress")
  public WatchHistoryResponse progress(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody ProgressRequest request
  ) {
    return watchHistoryService.upsertProgress(principal, id, request);
  }
}
