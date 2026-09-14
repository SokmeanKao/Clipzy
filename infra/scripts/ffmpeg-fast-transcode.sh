#!/bin/sh
# Fast HLS transcode: 1080p/720p/480p ladder + master playlist.
# Reference command used by TranscodeJobProcessor (Java ProcessBuilder).
#
# Changes vs. original medium-preset encode:
#   - preset veryfast → ~2-3x faster encode, slightly larger files
#   - +2-3% target bitrate to compensate for faster preset
#   - -threads 0 lets x264 use available cores for this encode
#
# Usage: ./ffmpeg-fast-transcode.sh <input> <output_dir>

INPUT="$1"
OUTPUT_DIR="$2"

mkdir -p "$OUTPUT_DIR"
cd "$OUTPUT_DIR" || exit 1

ffmpeg -y -i "$INPUT" \
  -filter_complex "[0:v]split=3[v1][v2][v3]; \
    [v1]scale=w=1920:h=1080[v1out]; [v2]scale=w=1280:h=720[v2out]; [v3]scale=w=854:h=480[v3out]" \
  -map "[v1out]" -c:v:0 libx264 -preset veryfast -b:v:0 5150k -threads 0 \
  -map a:0? -c:a:0 aac -b:a:0 192k \
  -map "[v2out]" -c:v:1 libx264 -preset veryfast -b:v:1 2880k -threads 0 \
  -map a:0? -c:a:1 aac -b:a:1 128k \
  -map "[v3out]" -c:v:2 libx264 -preset veryfast -b:v:2 1440k -threads 0 \
  -map a:0? -c:a:2 aac -b:a:2 96k \
  -var_stream_map "v:0,a:0 v:1,a:1 v:2,a:2" \
  -master_pl_name master.m3u8 \
  -f hls -hls_time 6 -hls_playlist_type vod \
  -hls_segment_filename "stream_%v/data%03d.ts" \
  -hls_flags independent_segments \
  "stream_%v/playlist.m3u8"
