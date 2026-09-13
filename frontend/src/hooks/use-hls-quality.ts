"use client";

import { useEffect, useState, useCallback } from "react";
import Hls from "hls.js";

export interface QualityLevel {
  index: number; // -1 reserved for "Auto"
  height: number;
  bitrate: number;
  label: string; // e.g. "1080p", "720p"
}

/**
 * Wires quality-selection state to an existing Hls instance.
 * Pass the same `hls` and `videoId` your player already creates.
 */
export function useHlsQuality(hls: Hls | null, videoId: string) {
  const [levels, setLevels] = useState<QualityLevel[]>([]);
  const [currentLevel, setCurrentLevel] = useState<number>(-1); // -1 = Auto
  const [activeLevel, setActiveLevel] = useState<number>(-1); // what's actually playing
  const storageKey = `clipzy:quality:${videoId}`;

  useEffect(() => {
    if (!hls) return;

    const onManifestParsed = () => {
      const parsed: QualityLevel[] = hls.levels
        .map((lvl, i) => ({
          index: i,
          height: lvl.height,
          bitrate: lvl.bitrate,
          label: lvl.height ? `${lvl.height}p` : `Level ${i + 1}`,
        }))
        .sort((a, b) => b.height - a.height);

      setLevels(parsed);

      const saved = localStorage.getItem(storageKey);
      const savedIndex = saved !== null ? Number(saved) : -1;
      const validSaved = parsed.some((l) => l.index === savedIndex);
      hls.currentLevel = validSaved ? savedIndex : -1;
      setCurrentLevel(hls.currentLevel);
      setActiveLevel(hls.currentLevel >= 0 ? hls.currentLevel : hls.loadLevel);
    };

    const onLevelSwitched = (_evt: unknown, data: { level: number }) => {
      setActiveLevel(data.level);
    };

    hls.on(Hls.Events.MANIFEST_PARSED, onManifestParsed);
    hls.on(Hls.Events.LEVEL_SWITCHED, onLevelSwitched);

    // If manifest already parsed (e.g. remount), hydrate immediately
    if (hls.levels.length > 0) {
      onManifestParsed();
    }

    return () => {
      hls.off(Hls.Events.MANIFEST_PARSED, onManifestParsed);
      hls.off(Hls.Events.LEVEL_SWITCHED, onLevelSwitched);
    };
  }, [hls, storageKey]);

  const selectLevel = useCallback(
    (index: number) => {
      if (!hls) return;
      hls.currentLevel = index;
      setCurrentLevel(index);
      localStorage.setItem(storageKey, String(index));
    },
    [hls, storageKey]
  );

  return { levels, currentLevel, activeLevel, selectLevel };
}
