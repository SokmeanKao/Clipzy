package com.clipzy.worker;

import com.clipzy.config.ClipzyProperties;
import com.clipzy.domain.JobStatus;
import com.clipzy.domain.Rendition;
import com.clipzy.domain.TranscodeJob;
import com.clipzy.domain.Video;
import com.clipzy.domain.VideoStatus;
import com.clipzy.repository.RenditionRepository;
import com.clipzy.repository.TranscodeJobRepository;
import com.clipzy.repository.VideoRepository;
import com.clipzy.service.StorageService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TranscodeJobProcessor {

  private static final Logger log = LoggerFactory.getLogger(TranscodeJobProcessor.class);

  private final TranscodeJobRepository jobRepository;
  private final VideoRepository videoRepository;
  private final RenditionRepository renditionRepository;
  private final StorageService storageService;
  private final ClipzyProperties properties;
  private final TransactionTemplate transactionTemplate;

  public TranscodeJobProcessor(
      TranscodeJobRepository jobRepository,
      VideoRepository videoRepository,
      RenditionRepository renditionRepository,
      StorageService storageService,
      ClipzyProperties properties,
      TransactionTemplate transactionTemplate
  ) {
    this.jobRepository = jobRepository;
    this.videoRepository = videoRepository;
    this.renditionRepository = renditionRepository;
    this.storageService = storageService;
    this.properties = properties;
    this.transactionTemplate = transactionTemplate;
  }

  public void process(UUID jobId) {
    UUID videoId = transactionTemplate.execute(status -> {
      TranscodeJob job = jobRepository.findById(jobId).orElse(null);
      if (job == null || job.getStatus() != JobStatus.PENDING) {
        return null;
      }
      job.setStatus(JobStatus.RUNNING);
      job.setUpdatedAt(Instant.now());
      jobRepository.save(job);
      return job.getVideo().getId();
    });
    if (videoId == null) {
      return;
    }

    Path workDir = null;
    try {
      workDir = Files.createTempDirectory(Path.of(System.getProperty("java.io.tmpdir")), "clipzy-");
      Path input = workDir.resolve("raw");
      storageService.download("videos/" + videoId + "/raw", input);

      Path hlsDir = workDir.resolve("hls");
      Files.createDirectories(hlsDir);
      runFfmpeg(input, hlsDir);

      String prefix = "videos/" + videoId + "/hls/";
      uploadTree(hlsDir, prefix);
      String masterPath = prefix + "master.m3u8";

      transactionTemplate.executeWithoutResult(status -> markSucceeded(jobId, videoId, masterPath, prefix));
      log.info("Transcode succeeded for video {}", videoId);
    } catch (Exception e) {
      log.error("Transcode failed for video {}: {}", videoId, e.getMessage(), e);
      String message = truncate(e.getMessage(), 4000);
      transactionTemplate.executeWithoutResult(status -> markFailed(jobId, videoId, message));
    } finally {
      if (workDir != null) {
        deleteRecursive(workDir);
      }
    }
  }

  @Transactional
  protected void markSucceeded(UUID jobId, UUID videoId, String masterPath, String prefix) {
    Video video = videoRepository.findById(videoId).orElseThrow();
    video.setStatus(VideoStatus.READY);
    video.setManifestPath(masterPath);
    if (video.getPublishedAt() == null) {
      video.setPublishedAt(Instant.now());
    }
    videoRepository.save(video);

    saveRendition(video, 1080, 5000, prefix + "stream_0/playlist.m3u8");
    saveRendition(video, 720, 2800, prefix + "stream_1/playlist.m3u8");
    saveRendition(video, 480, 1400, prefix + "stream_2/playlist.m3u8");

    TranscodeJob job = jobRepository.findById(jobId).orElseThrow();
    job.setStatus(JobStatus.SUCCEEDED);
    job.setErrorMessage(null);
    job.setUpdatedAt(Instant.now());
    jobRepository.save(job);
  }

  @Transactional
  protected void markFailed(UUID jobId, UUID videoId, String message) {
    videoRepository.findById(videoId).ifPresent(video -> {
      video.setStatus(VideoStatus.FAILED);
      videoRepository.save(video);
    });
    jobRepository.findById(jobId).ifPresent(job -> {
      job.setStatus(JobStatus.FAILED);
      job.setErrorMessage(message);
      job.setUpdatedAt(Instant.now());
      jobRepository.save(job);
    });
  }

  private void runFfmpeg(Path input, Path hlsDir) throws IOException, InterruptedException {
    String ffmpeg = properties.getFfmpeg().getPath();
    List<String> command = List.of(
        ffmpeg, "-y", "-i", input.toAbsolutePath().toString(),
        "-filter_complex",
        "[0:v]split=3[v1][v2][v3]; [v1]scale=w=1920:h=1080[v1out]; [v2]scale=w=1280:h=720[v2out]; [v3]scale=w=854:h=480[v3out]",
        "-map", "[v1out]", "-c:v:0", "libx264", "-b:v:0", "5000k", "-map", "a:0?", "-c:a:0", "aac", "-b:a:0", "192k",
        "-map", "[v2out]", "-c:v:1", "libx264", "-b:v:1", "2800k", "-map", "a:0?", "-c:a:1", "aac", "-b:a:1", "128k",
        "-map", "[v3out]", "-c:v:2", "libx264", "-b:v:2", "1400k", "-map", "a:0?", "-c:a:2", "aac", "-b:a:2", "96k",
        "-var_stream_map", "v:0,a:0 v:1,a:1 v:2,a:2",
        "-master_pl_name", "master.m3u8",
        "-f", "hls",
        "-hls_time", "4",
        "-hls_playlist_type", "vod",
        "-hls_segment_filename", "stream_%v/data%03d.ts",
        "-hls_flags", "independent_segments",
        "stream_%v/playlist.m3u8"
    );

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(hlsDir.toFile());
    pb.redirectErrorStream(true);
    Process process = pb.start();
    StringBuilder output = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append('\n');
      }
    }
    boolean finished = process.waitFor(2, TimeUnit.HOURS);
    if (!finished) {
      process.destroyForcibly();
      throw new IllegalStateException("FFmpeg timed out\n" + output);
    }
    if (process.exitValue() != 0) {
      log.error("FFmpeg stderr/stdout:\n{}", output);
      throw new IllegalStateException("FFmpeg exited with code " + process.exitValue() + "\n" + output);
    }
  }

  private void uploadTree(Path root, String keyPrefix) throws IOException {
    try (Stream<Path> walk = Files.walk(root)) {
      walk.filter(Files::isRegularFile).forEach(file -> {
        String relative = root.relativize(file).toString().replace('\\', '/');
        storageService.uploadFile(keyPrefix + relative, file, contentTypeFor(relative));
      });
    }
  }

  private void saveRendition(Video video, int height, int bitrateKbps, String path) {
    Rendition rendition = new Rendition();
    rendition.setVideo(video);
    rendition.setHeight(height);
    rendition.setBitrateKbps(bitrateKbps);
    rendition.setStoragePath(path);
    renditionRepository.save(rendition);
  }

  private static String contentTypeFor(String relative) {
    if (relative.endsWith(".m3u8")) {
      return "application/vnd.apple.mpegurl";
    }
    if (relative.endsWith(".ts")) {
      return "video/mp2t";
    }
    return "application/octet-stream";
  }

  private static void deleteRecursive(Path root) {
    try (Stream<Path> walk = Files.walk(root)) {
      walk.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.deleteIfExists(path);
        } catch (IOException ignored) {
          // best-effort cleanup
        }
      });
    } catch (IOException ignored) {
      // best-effort cleanup
    }
  }

  private static String truncate(String message, int max) {
    if (message == null) {
      return null;
    }
    return message.length() <= max ? message : message.substring(0, max);
  }
}
