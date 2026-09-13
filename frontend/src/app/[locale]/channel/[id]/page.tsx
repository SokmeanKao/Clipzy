"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api } from "@/lib/api";
import type { Channel, Video } from "@/lib/types";
import { normalizeVideos } from "@/lib/types";
import { useAuth } from "@/components/auth-provider";
import { VideoCard, VideoCardSkeleton } from "@/components/video-card";
import { EmptyState, ErrorState } from "@/components/ui-states";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

export default function ChannelPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const { isAuthenticated, user } = useAuth();

  const [channel, setChannel] = useState<Channel | null>(null);
  const [videos, setVideos] = useState<Video[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.channel(id);
      setChannel(data);
      // Backend returns Spring Page under `videos`, not a bare array
      setVideos(normalizeVideos(data.videos));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load channel");
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  const subscribed = !!(channel?.subscribed ?? channel?.isSubscribed);
  const channelId = channel?.id ?? id;
  const isOwn = user?.id === (channel?.userId ?? channel?.id ?? id);

  const toggleSubscribe = async () => {
    if (!isAuthenticated || !channel) return;
    setBusy(true);
    try {
      if (subscribed) await api.unsubscribe(channelId);
      else await api.subscribe(channelId);
      setChannel({
        ...channel,
        subscribed: !subscribed,
        isSubscribed: !subscribed,
        subscriberCount: Math.max(
          0,
          (channel.subscriberCount ?? 0) + (subscribed ? -1 : 1)
        ),
      });
    } catch {
      /* keep prior state */
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return (
      <div className="space-y-8">
        <div className="flex items-center gap-4">
          <Skeleton className="size-20 rounded-full" />
          <div className="space-y-2">
            <Skeleton className="h-7 w-48" />
            <Skeleton className="h-4 w-32" />
          </div>
        </div>
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <VideoCardSkeleton key={i} />
          ))}
        </div>
      </div>
    );
  }

  if (error || !channel) {
    return (
      <ErrorState
        title="Channel unavailable"
        description={error ?? "Not found"}
        onRetry={() => void load()}
      />
    );
  }

  return (
    <div className="space-y-8 animate-soft-in">
      <section className="flex flex-col gap-5 rounded-2xl border border-border/70 bg-card/50 p-6 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <Avatar className="size-20">
            <AvatarImage src={channel.avatarUrl ?? undefined} alt="" />
            <AvatarFallback className="text-2xl">
              {(channel.displayName?.[0] ?? "C").toUpperCase()}
            </AvatarFallback>
          </Avatar>
          <div className="space-y-1">
            <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">
              {channel.displayName}
            </h1>
            <p className="text-sm text-muted-foreground">
              {(channel.subscriberCount ?? 0).toLocaleString()} subscribers
            </p>
            {(channel.bio || channel.description) && (
              <p className="max-w-xl text-sm text-muted-foreground">
                {channel.bio ?? channel.description}
              </p>
            )}
          </div>
        </div>
        {!isOwn && (
          <Button
            onClick={() => void toggleSubscribe()}
            disabled={!isAuthenticated || busy}
            variant={subscribed ? "outline" : "default"}
          >
            {!isAuthenticated
              ? "Log in to subscribe"
              : busy
                ? "…"
                : subscribed
                  ? "Subscribed"
                  : "Subscribe"}
          </Button>
        )}
      </section>

      <section className="space-y-4">
        <h2 className="text-lg font-semibold">Videos</h2>
        {videos.length === 0 ? (
          <EmptyState
            title="No videos on this channel"
            description="Uploads will show up here when ready."
          />
        ) : (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {videos.map((v) => (
              <VideoCard key={v.id} video={v} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
