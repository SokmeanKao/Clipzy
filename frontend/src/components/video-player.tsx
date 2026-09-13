"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Hls from "hls.js";
import {
  ChevronLeft,
  ChevronRight,
  Maximize2,
  Minimize2,
  Pause,
  Play,
  Settings,
  Volume2,
  VolumeX,
} from "lucide-react";
import { api } from "@/lib/api";
import { useHlsQuality } from "@/hooks/use-hls-quality";
import { Button } from "@/components/ui/button";
import { Slider } from "@/components/ui/slider";
import { cn } from "@/lib/utils";

const SPEEDS = [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2];
const SPEED_SESSION_KEY = "clipzy:playback-speed";

type SettingsPanel = "closed" | "root" | "speed" | "quality";

type VideoPlayerProps = {
  videoId: string;
  src?: string | null;
  poster?: string | null;
  initialProgress?: number;
  className?: string;
};

function formatTime(seconds: number) {
  if (!Number.isFinite(seconds) || seconds < 0) return "0:00";
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, "0")}`;
}

export function VideoPlayer({
  videoId,
  src,
  poster,
  initialProgress = 0,
  className,
}: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const progressTimer = useRef<ReturnType<typeof setInterval> | null>(null);
  const viewRecorded = useRef(false);
  const resumeApplied = useRef(false);

  const [hls, setHls] = useState<Hls | null>(null);
  const { levels, currentLevel, activeLevel, selectLevel } = useHlsQuality(
    hls,
    videoId
  );

  const [playing, setPlaying] = useState(false);
  const [current, setCurrent] = useState(0);
  const [duration, setDuration] = useState(0);
  const [buffered, setBuffered] = useState(0);
  const [volume, setVolume] = useState(1);
  const [muted, setMuted] = useState(false);
  const [speed, setSpeed] = useState(1);
  const [fullscreen, setFullscreen] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [settingsPanel, setSettingsPanel] = useState<SettingsPanel>("closed");
  const hideTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const settingsOpenRef = useRef(false);

  const activeLabel =
    levels.find((l) => l.index === activeLevel)?.label ??
    (activeLevel >= 0 ? `${activeLevel}` : null);

  const qualityTriggerLabel =
    currentLevel === -1
      ? activeLabel
        ? `Auto (${activeLabel})`
        : "Auto"
      : levels.find((l) => l.index === currentLevel)?.label ?? "Quality";

  useEffect(() => {
    settingsOpenRef.current = settingsPanel !== "closed";
  }, [settingsPanel]);

  const bumpControls = useCallback(() => {
    setShowControls(true);
    if (hideTimer.current) clearTimeout(hideTimer.current);
    hideTimer.current = setTimeout(() => {
      if (settingsOpenRef.current) return;
      if (videoRef.current && !videoRef.current.paused) {
        setShowControls(false);
      }
    }, 2500);
  }, []);

  const reportProgress = useCallback(() => {
    const el = videoRef.current;
    if (!el || el.paused) return;
    void api.reportProgress(videoId, Math.floor(el.currentTime)).catch(() => {
      /* optional while unauthenticated */
    });
  }, [videoId]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !src) return;

    resumeApplied.current = false;
    viewRecorded.current = false;

    setHls((prev) => {
      prev?.destroy();
      return null;
    });

    if (src.includes(".m3u8") && Hls.isSupported()) {
      const instance = new Hls({
        enableWorker: true,
        startLevel: -1,
      });
      instance.loadSource(src);
      instance.attachMedia(video);
      setHls(instance);
    } else if (
      video.canPlayType("application/vnd.apple.mpegurl") ||
      !src.includes(".m3u8")
    ) {
      video.src = src;
    }

    return () => {
      setHls((prev) => {
        prev?.destroy();
        return null;
      });
    };
  }, [src]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const savedSpeed = sessionStorage.getItem(SPEED_SESSION_KEY);
    if (savedSpeed) {
      const rate = Number(savedSpeed);
      if (SPEEDS.includes(rate)) {
        video.playbackRate = rate;
        setSpeed(rate);
      }
    }

    const onTime = () => {
      setCurrent(video.currentTime);
      if (video.buffered.length > 0) {
        setBuffered(video.buffered.end(video.buffered.length - 1));
      }
    };
    const onMeta = () => {
      setDuration(video.duration || 0);
      if (!resumeApplied.current && initialProgress > 0) {
        const target = Math.min(initialProgress, Math.max(video.duration - 1, 0));
        if (target > 2) {
          video.currentTime = target;
        }
        resumeApplied.current = true;
      }
    };
    const onPlay = () => {
      setPlaying(true);
      bumpControls();
      if (!viewRecorded.current) {
        viewRecorded.current = true;
        void api.recordView(videoId).catch(() => undefined);
      }
      if (progressTimer.current) clearInterval(progressTimer.current);
      progressTimer.current = setInterval(reportProgress, 10_000);
      reportProgress();
    };
    const onPause = () => {
      setPlaying(false);
      setShowControls(true);
      if (progressTimer.current) {
        clearInterval(progressTimer.current);
        progressTimer.current = null;
      }
      reportProgress();
    };
    const onVol = () => {
      setVolume(video.volume);
      setMuted(video.muted);
    };

    video.addEventListener("timeupdate", onTime);
    video.addEventListener("loadedmetadata", onMeta);
    video.addEventListener("play", onPlay);
    video.addEventListener("pause", onPause);
    video.addEventListener("volumechange", onVol);

    return () => {
      video.removeEventListener("timeupdate", onTime);
      video.removeEventListener("loadedmetadata", onMeta);
      video.removeEventListener("play", onPlay);
      video.removeEventListener("pause", onPause);
      video.removeEventListener("volumechange", onVol);
      if (progressTimer.current) clearInterval(progressTimer.current);
    };
  }, [videoId, initialProgress, reportProgress, bumpControls]);

  useEffect(() => {
    const onFs = () => {
      setFullscreen(!!document.fullscreenElement);
      setSettingsPanel("closed");
    };
    document.addEventListener("fullscreenchange", onFs);
    return () => document.removeEventListener("fullscreenchange", onFs);
  }, []);

  const togglePlay = () => {
    const video = videoRef.current;
    if (!video) return;
    if (video.paused) void video.play();
    else video.pause();
  };

  const seek = (value: number[]) => {
    const video = videoRef.current;
    if (!video) return;
    video.currentTime = value[0] ?? 0;
    setCurrent(video.currentTime);
  };

  const changeVolume = (value: number[]) => {
    const video = videoRef.current;
    if (!video) return;
    const v = value[0] ?? 0;
    video.volume = v;
    video.muted = v === 0;
    setVolume(v);
    setMuted(v === 0);
  };

  const changeSpeed = (rate: number) => {
    const video = videoRef.current;
    if (!video) return;
    video.playbackRate = rate;
    setSpeed(rate);
    sessionStorage.setItem(SPEED_SESSION_KEY, String(rate));
    setSettingsPanel("closed");
  };

  const changeQuality = (index: number) => {
    selectLevel(index);
    setSettingsPanel("closed");
  };

  const toggleFullscreen = async () => {
    const el = containerRef.current;
    if (!el) return;
    setSettingsPanel("closed");
    if (!document.fullscreenElement) await el.requestFullscreen();
    else await document.exitFullscreen();
  };

  const toggleSettings = () => {
    setSettingsPanel((prev) => (prev === "closed" ? "root" : "closed"));
    setShowControls(true);
  };

  if (!src) {
    return (
      <div
        className={cn(
          "flex aspect-video items-center justify-center rounded-xl bg-ink text-sm text-ink-foreground/70",
          className
        )}
      >
        Video not ready for playback
      </div>
    );
  }

  const menuItemClass =
    "flex w-full cursor-pointer items-center gap-2 rounded-md px-2.5 py-2 text-left text-sm text-white hover:bg-white/15";

  return (
    <div
      ref={containerRef}
      className={cn(
        "group relative overflow-hidden rounded-xl bg-ink shadow-[0_20px_50px_-28px_rgba(15,23,42,0.55)]",
        className
      )}
      onMouseMove={bumpControls}
      onMouseLeave={() => {
        if (settingsPanel !== "closed") return;
        if (playing) setShowControls(false);
      }}
    >
      <video
        ref={videoRef}
        className="aspect-video w-full bg-black"
        poster={poster ?? undefined}
        playsInline
        onClick={() => {
          if (settingsPanel !== "closed") {
            setSettingsPanel("closed");
            return;
          }
          togglePlay();
        }}
        controls={false}
      />

      {/* Inline settings — must live inside the fullscreen element (no document portal) */}
      {settingsPanel !== "closed" && (
        <div
          className="pointer-events-auto absolute right-3 bottom-16 z-30 w-56 overflow-hidden rounded-lg bg-black/90 text-white shadow-lg ring-1 ring-white/15 backdrop-blur-md"
          onClick={(e) => e.stopPropagation()}
        >
          {settingsPanel === "root" && (
            <div className="p-1">
              <button
                type="button"
                className={menuItemClass}
                onClick={() => setSettingsPanel("speed")}
              >
                <span>Speed</span>
                <span className="ml-auto text-xs text-white/60">
                  {speed === 1 ? "Normal" : `${speed}x`}
                </span>
                <ChevronRight className="size-4 opacity-70" />
              </button>
              {levels.length > 0 && (
                <button
                  type="button"
                  className={menuItemClass}
                  onClick={() => setSettingsPanel("quality")}
                >
                  <span>Quality</span>
                  <span className="ml-auto max-w-[6.5rem] truncate text-xs text-white/60">
                    {qualityTriggerLabel}
                  </span>
                  <ChevronRight className="size-4 opacity-70" />
                </button>
              )}
            </div>
          )}

          {settingsPanel === "speed" && (
            <div className="p-1">
              <button
                type="button"
                className={menuItemClass}
                onClick={() => setSettingsPanel("root")}
              >
                <ChevronLeft className="size-4" />
                <span className="font-medium">Speed</span>
              </button>
              <div className="my-1 h-px bg-white/10" />
              {SPEEDS.map((s) => (
                <button
                  key={s}
                  type="button"
                  className={menuItemClass}
                  onClick={() => changeSpeed(s)}
                >
                  {s === 1 ? "Normal" : `${s}x`}
                  {speed === s ? <span className="ml-auto text-xs">✓</span> : null}
                </button>
              ))}
            </div>
          )}

          {settingsPanel === "quality" && (
            <div className="p-1">
              <button
                type="button"
                className={menuItemClass}
                onClick={() => setSettingsPanel("root")}
              >
                <ChevronLeft className="size-4" />
                <span className="font-medium">Quality</span>
              </button>
              <div className="my-1 h-px bg-white/10" />
              <button
                type="button"
                className={menuItemClass}
                onClick={() => changeQuality(-1)}
              >
                {activeLabel ? `Auto (${activeLabel})` : "Auto"}
                {currentLevel === -1 ? (
                  <span className="ml-auto text-xs">✓</span>
                ) : null}
              </button>
              {levels.map((q) => (
                <button
                  key={q.index}
                  type="button"
                  className={menuItemClass}
                  onClick={() => changeQuality(q.index)}
                >
                  {q.label}
                  {currentLevel === q.index ? (
                    <span className="ml-auto text-xs">✓</span>
                  ) : null}
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      <div
        className={cn(
          "pointer-events-none absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent px-3 pb-3 pt-16 transition-opacity duration-300",
          showControls || settingsPanel !== "closed" ? "opacity-100" : "opacity-0"
        )}
      >
        <div className="pointer-events-auto space-y-2">
          <div className="relative h-1.5 overflow-hidden rounded-full bg-white/20">
            <div
              className="absolute inset-y-0 left-0 bg-white/35"
              style={{
                width: duration
                  ? `${Math.min(100, (buffered / duration) * 100)}%`
                  : "0%",
              }}
            />
            <Slider
              value={[current]}
              max={duration || 1}
              step={0.1}
              onValueChange={seek}
              className="absolute inset-0 **:[[role=slider]]:size-3 **:data-[slot=slider-track]:bg-transparent **:data-[slot=slider-range]:bg-primary"
              aria-label="Seek"
            />
          </div>

          <div className="flex flex-wrap items-center gap-1.5 text-white">
            <Button
              type="button"
              size="icon-sm"
              variant="ghost"
              className="text-white hover:bg-white/15 hover:text-white"
              onClick={togglePlay}
              aria-label={playing ? "Pause" : "Play"}
            >
              {playing ? <Pause /> : <Play />}
            </Button>

            <span className="min-w-[5.5rem] px-1 text-xs tabular-nums text-white/90">
              {formatTime(current)} / {formatTime(duration)}
            </span>

            <div className="ml-1 flex items-center gap-1">
              <Button
                type="button"
                size="icon-sm"
                variant="ghost"
                className="text-white hover:bg-white/15 hover:text-white"
                onClick={() => {
                  const video = videoRef.current;
                  if (!video) return;
                  video.muted = !video.muted;
                  setMuted(video.muted);
                }}
                aria-label={muted ? "Unmute" : "Mute"}
              >
                {muted || volume === 0 ? <VolumeX /> : <Volume2 />}
              </Button>
              <Slider
                value={[muted ? 0 : volume]}
                max={1}
                step={0.05}
                onValueChange={changeVolume}
                className="w-20 **:[[role=slider]]:size-3"
                aria-label="Volume"
              />
            </div>

            <div className="ml-auto flex items-center gap-1">
              <Button
                type="button"
                size="icon-sm"
                variant="ghost"
                className={cn(
                  "text-white hover:bg-white/15 hover:text-white",
                  settingsPanel !== "closed" && "bg-white/15"
                )}
                onClick={toggleSettings}
                aria-label="Settings"
                aria-expanded={settingsPanel !== "closed"}
              >
                <Settings />
              </Button>

              <Button
                type="button"
                size="icon-sm"
                variant="ghost"
                className="text-white hover:bg-white/15 hover:text-white"
                onClick={() => void toggleFullscreen()}
                aria-label={fullscreen ? "Exit fullscreen" : "Fullscreen"}
              >
                {fullscreen ? <Minimize2 /> : <Maximize2 />}
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
