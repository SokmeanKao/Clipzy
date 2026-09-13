package com.clipzy.service;

import com.clipzy.domain.Comment;
import com.clipzy.domain.User;
import com.clipzy.domain.Video;
import com.clipzy.dto.CommentRequest;
import com.clipzy.dto.CommentResponse;
import com.clipzy.repository.CommentRepository;
import com.clipzy.repository.UserRepository;
import com.clipzy.repository.VideoRepository;
import com.clipzy.security.UserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentService {

  private final CommentRepository commentRepository;
  private final VideoRepository videoRepository;
  private final UserRepository userRepository;
  private final VideoService videoService;

  public CommentService(
      CommentRepository commentRepository,
      VideoRepository videoRepository,
      UserRepository userRepository,
      VideoService videoService
  ) {
    this.commentRepository = commentRepository;
    this.videoRepository = videoRepository;
    this.userRepository = userRepository;
    this.videoService = videoService;
  }

  @Transactional(readOnly = true)
  public List<CommentResponse> list(UUID videoId, UserPrincipal principal) {
    Video video = videoRepository.findById(videoId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));
    videoService.assertCanView(video, principal);
    return commentRepository.findByVideoIdOrderByCreatedAtAsc(videoId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public CommentResponse create(UserPrincipal principal, UUID videoId, CommentRequest request) {
    Video video = videoRepository.findById(videoId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));
    videoService.assertCanView(video, principal);
    User user = userRepository.findById(principal.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

    Comment comment = new Comment();
    comment.setVideo(video);
    comment.setUser(user);
    comment.setBody(request.body().trim());
    if (request.parentId() != null) {
      Comment parent = commentRepository.findById(request.parentId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent comment not found"));
      if (!parent.getVideo().getId().equals(videoId)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent belongs to another video");
      }
      comment.setParent(parent);
    }
    commentRepository.save(comment);
    return toResponse(comment);
  }

  private CommentResponse toResponse(Comment comment) {
    return new CommentResponse(
        comment.getId(),
        comment.getVideo().getId(),
        comment.getUser().getId(),
        comment.getUser().getDisplayName(),
        comment.getParent() == null ? null : comment.getParent().getId(),
        comment.getBody(),
        comment.getCreatedAt()
    );
  }
}
