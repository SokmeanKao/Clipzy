package com.clipzy.service;

import com.clipzy.config.ClipzyProperties;
import com.clipzy.domain.JobStatus;
import com.clipzy.domain.TranscodeJob;
import com.clipzy.domain.User;
import com.clipzy.domain.Video;
import com.clipzy.domain.VideoStatus;
import com.clipzy.domain.VideoVisibility;
import com.clipzy.dto.CreateVideoRequest;
import com.clipzy.dto.CreateVideoResponse;
import com.clipzy.dto.VideoResponse;
import com.clipzy.repository.TranscodeJobRepository;
import com.clipzy.repository.UserRepository;
import com.clipzy.repository.VideoRepository;
import com.clipzy.security.UserPrincipal;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class VideoService {

  private final VideoRepository videoRepository;
  private final UserRepository userRepository;
  private final TranscodeJobRepository transcodeJobRepository;
  private final S3Presigner s3Presigner;
  private final ClipzyProperties properties;
  private final ViewCountService viewCountService;

  public VideoService(
      VideoRepository videoRepository,
      UserRepository userRepository,
      TranscodeJobRepository transcodeJobRepository,
      S3Presigner s3Presigner,
      ClipzyProperties properties,
      ViewCountService viewCountService
  ) {
    this.videoRepository = videoRepository;
    this.userRepository = userRepository;
    this.transcodeJobRepository = transcodeJobRepository;
    this.s3Presigner = s3Presigner;
    this.properties = properties;
    this.viewCountService = viewCountService;
  }

  @Transactional
  public CreateVideoResponse create(UserPrincipal principal, CreateVideoRequest request) {
    User owner = userRepository.findById(principal.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

    Video video = new Video();
    video.setOwner(owner);
    video.setTitle(request.title().trim());
    video.setDescription(request.description() == null ? null : request.description().trim());
    video.setVisibility(request.visibility() == null ? VideoVisibility.PUBLIC : request.visibility());
    video.setStatus(VideoStatus.UPLOADING);
    videoRepository.save(video);

    String objectKey = "videos/" + video.getId() + "/raw";
    String uploadUrl = presignPut(objectKey);

    return new CreateVideoResponse(
        video.getId(),
        uploadUrl,
        objectKey,
        video.getStatus().toDb()
    );
  }

  @Transactional
  public VideoResponse completeUpload(UserPrincipal principal, UUID videoId) {
    Video video = requireOwned(principal, videoId);
    if (video.getStatus() != VideoStatus.UPLOADING && video.getStatus() != VideoStatus.FAILED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Video is not awaiting upload");
    }
    video.setStatus(VideoStatus.PROCESSING);

    TranscodeJob job = new TranscodeJob();
    job.setVideo(video);
    job.setStatus(JobStatus.PENDING);
    transcodeJobRepository.save(job);

    return toResponse(video);
  }

  @Transactional(readOnly = true)
  public VideoResponse getById(UUID videoId, UserPrincipal principal) {
    Video video = videoRepository.findById(videoId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));
    assertCanView(video, principal);
    return toResponse(video);
  }

  @Transactional(readOnly = true)
  public Page<VideoResponse> publicFeed(Pageable pageable) {
    return videoRepository
        .findByStatusAndVisibilityOrderByCreatedAtDesc(
            VideoStatus.READY, VideoVisibility.PUBLIC, pageable)
        .map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public Page<VideoResponse> search(String q, Pageable pageable) {
    if (q == null || q.isBlank()) {
      return publicFeed(pageable);
    }
    return videoRepository.searchPublicReady(q.trim(), pageable).map(this::toResponse);
  }

  public void recordView(UUID videoId) {
    viewCountService.incrementAsync(videoId);
  }

  public String publicObjectUrl(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      return null;
    }
    String path = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
    return properties.getS3().getEndpoint().replaceAll("/$", "")
        + "/" + properties.getS3().getBucket() + "/" + path;
  }

  public VideoResponse toResponse(Video video) {
    String manifestUrl = null;
    if (video.getManifestPath() != null) {
      manifestUrl = publicObjectUrl(video.getManifestPath());
    }
    return new VideoResponse(
        video.getId(),
        video.getOwner().getId(),
        video.getOwner().getDisplayName(),
        video.getTitle(),
        video.getDescription(),
        video.getStatus(),
        video.getVisibility(),
        video.getManifestPath(),
        manifestUrl,
        video.getThumbnailPath(),
        video.getViewCount(),
        video.getPublishedAt(),
        video.getCreatedAt()
    );
  }

  Video requireOwned(UserPrincipal principal, UUID videoId) {
    Video video = videoRepository.findById(videoId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));
    if (!video.getOwner().getId().equals(principal.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the video owner");
    }
    return video;
  }

  public void assertCanView(Video video, UserPrincipal principal) {
    boolean owner = principal != null && video.getOwner().getId().equals(principal.getId());
    if (owner) {
      return;
    }
    if (video.getVisibility() == VideoVisibility.PRIVATE) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Private video");
    }
    if (video.getVisibility() == VideoVisibility.PUBLIC && video.getStatus() == VideoStatus.READY) {
      return;
    }
    if (video.getVisibility() == VideoVisibility.UNLISTED && video.getStatus() == VideoStatus.READY) {
      return;
    }
    if (video.getStatus() != VideoStatus.READY) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Video not available");
    }
  }

  private String presignPut(String objectKey) {
    PutObjectRequest objectRequest = PutObjectRequest.builder()
        .bucket(properties.getS3().getBucket())
        .key(objectKey)
        .build();
    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(properties.getS3().getPresignPutMinutes()))
        .putObjectRequest(objectRequest)
        .build();
    return s3Presigner.presignPutObject(presignRequest).url().toString();
  }
}
