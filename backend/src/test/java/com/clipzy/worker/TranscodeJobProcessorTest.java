package com.clipzy.worker;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class TranscodeJobProcessorTest {

  @Test
  void ffmpegCommand_usesVeryfastPresetAndThreads() {
    List<String> cmd = TranscodeJobProcessor.buildFfmpegCommand(
        "ffmpeg", Path.of("in.mp4"), Path.of("out"));
    assertTrue(cmd.contains("veryfast"));
    assertTrue(cmd.contains("5150k"));
    assertTrue(cmd.contains("2880k"));
    assertTrue(cmd.contains("1440k"));
    assertTrue(cmd.contains("-threads"));
    assertTrue(cmd.contains("0"));
  }
}
