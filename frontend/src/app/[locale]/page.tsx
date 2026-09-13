"use client";

import { useCallback, useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { api } from "@/lib/api";
import type { Video } from "@/lib/types";
import { normalizeVideos } from "@/lib/types";
import { VideoCard, VideoCardSkeleton } from "@/components/video-card";
import { EmptyState, ErrorState } from "@/components/ui-states";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export default function HomePage() {
  const t = useTranslations("home");
  const tNav = useTranslations("nav");
  const [videos, setVideos] = useState<Video[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [searching, setSearching] = useState(false);

  const loadFeed = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.videos(0, 24);
      setVideos(normalizeVideos(data));
    } catch (e) {
      setError(e instanceof Error ? e.message : t("errorTitle"));
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    void loadFeed();
  }, [loadFeed]);

  const onSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    const q = query.trim();
    if (!q) {
      void loadFeed();
      return;
    }
    setSearching(true);
    setError(null);
    try {
      const data = await api.search(q);
      setVideos(normalizeVideos(data));
    } catch (err) {
      setError(err instanceof Error ? err.message : t("errorTitle"));
    } finally {
      setSearching(false);
    }
  };

  return (
    <div className="space-y-10">
      <section className="animate-fade-up relative overflow-hidden rounded-[1.75rem] border border-border/70 bg-[linear-gradient(135deg,color-mix(in_oklch,var(--card)_92%,white),color-mix(in_oklch,var(--primary)_8%,var(--card)))] px-6 py-12 sm:px-10 sm:py-16">
        <div className="pointer-events-none absolute -right-16 -top-16 size-56 rounded-full bg-primary/15 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-20 left-10 size-48 rounded-full bg-ink/5 blur-3xl" />
        <div className="relative max-w-xl space-y-4">
          <h1 className="font-heading text-5xl font-semibold tracking-tight text-foreground sm:text-6xl">
            {t("brand")}
          </h1>
          <p className="max-w-md text-base leading-relaxed text-muted-foreground sm:text-lg">
            {t("tagline")}
          </p>
          <div className="flex flex-wrap gap-2 pt-1">
            <Button asChild size="lg">
              <Link href="/upload">{tNav("upload")}</Link>
            </Button>
            <Button asChild size="lg" variant="outline">
              <Link href="/register">{tNav("register")}</Link>
            </Button>
          </div>
        </div>
      </section>

      <section
        className="animate-soft-in space-y-5"
        style={{ animationDelay: "120ms" }}
      >
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <form onSubmit={onSearch} className="flex w-full gap-2 sm:max-w-sm sm:ml-auto">
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={t("searchPlaceholder")}
              aria-label={t("search")}
            />
            <Button type="submit" variant="secondary" disabled={searching}>
              {searching ? "…" : t("search")}
            </Button>
          </form>
        </div>

        {loading ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <VideoCardSkeleton key={i} />
            ))}
          </div>
        ) : error ? (
          <ErrorState
            title={t("errorTitle")}
            description={error}
            onRetry={() => void loadFeed()}
          />
        ) : videos.length === 0 ? (
          <EmptyState
            title={t("emptyTitle")}
            description={t("emptyDescription")}
            action={
              <Button asChild>
                <Link href="/upload">{tNav("upload")}</Link>
              </Button>
            }
          />
        ) : (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {videos.map((video, i) => (
              <div
                key={video.id}
                className="animate-fade-up"
                style={{ animationDelay: `${Math.min(i, 8) * 40}ms` }}
              >
                <VideoCard video={video} />
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
