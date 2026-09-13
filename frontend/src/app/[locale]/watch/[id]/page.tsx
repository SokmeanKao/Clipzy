"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "@/i18n/navigation";
import { useParams } from "next/navigation";
import { api, getAccessToken } from "@/lib/api";
import type { Comment, Video, WatchHistoryItem } from "@/lib/types";
import {
  commentAuthor,
  commentText,
  unwrapList,
} from "@/lib/types";
import { VideoPlayer } from "@/components/video-player";
import { VideoCard, VideoCardSkeleton } from "@/components/video-card";
import { EmptyState, ErrorState } from "@/components/ui-states";
import { useAuth } from "@/components/auth-provider";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";

export default function WatchPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const { isAuthenticated } = useAuth();

  const [video, setVideo] = useState<Video | null>(null);
  const [related, setRelated] = useState<Video[]>([]);
  const [comments, setComments] = useState<Comment[]>([]);
  const [resumeAt, setResumeAt] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [commentBody, setCommentBody] = useState("");
  const [posting, setPosting] = useState(false);
  const [commentError, setCommentError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [v, feed] = await Promise.all([
        api.video(id),
        api.videos(0, 12),
      ]);
      setVideo(v);
      setRelated(unwrapList(feed).filter((item) => item.id !== id).slice(0, 8));

      try {
        setComments(unwrapList(await api.comments(id)));
      } catch {
        setComments([]);
      }

      if (getAccessToken()) {
        try {
          const history = unwrapList(await api.history());
          const match = history.find((h: WatchHistoryItem) => {
            const vid = h.videoId ?? h.video?.id;
            return vid === id;
          });
          const seconds =
            match?.progressSeconds ??
            match?.watchedSeconds ??
            match?.positionSeconds ??
            match?.video?.progressSeconds ??
            0;
          setResumeAt(typeof seconds === "number" ? seconds : 0);
        } catch {
          setResumeAt(0);
        }
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load video");
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  const owner = video?.owner;

  const viewsLabel = useMemo(() => {
    if (video?.viewCount == null) return null;
    return `${video.viewCount.toLocaleString()} views`;
  }, [video?.viewCount]);

  const submitComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentBody.trim()) return;
    setPosting(true);
    setCommentError(null);
    try {
      const created = await api.postComment(id, commentBody.trim());
      setComments((prev) => [created, ...prev]);
      setCommentBody("");
    } catch (err) {
      setCommentError(
        err instanceof Error ? err.message : "Could not post comment"
      );
    } finally {
      setPosting(false);
    }
  };

  if (loading) {
    return (
      <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="space-y-4">
          <Skeleton className="aspect-video w-full rounded-xl" />
          <Skeleton className="h-7 w-2/3" />
          <Skeleton className="h-4 w-1/3" />
        </div>
        <div className="space-y-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <VideoCardSkeleton key={i} compact />
          ))}
        </div>
      </div>
    );
  }

  if (error || !video) {
    return (
      <ErrorState
        title="Video unavailable"
        description={error ?? "Not found"}
        onRetry={() => void load()}
      />
    );
  }

  return (
    <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_320px]">
      <div className="space-y-5 animate-soft-in">
        <VideoPlayer
          videoId={video.id}
          src={video.manifestUrl}
          poster={video.thumbnailUrl}
          initialProgress={resumeAt}
        />

        <div className="space-y-3">
          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">
            {video.title}
          </h1>
          <div className="flex flex-wrap items-center gap-3">
            {owner && (
              <Link
                href={`/channel/${owner.id}`}
                className="flex items-center gap-2 rounded-full pr-2 transition hover:bg-muted"
              >
                <Avatar className="size-9">
                  <AvatarImage src={owner.avatarUrl ?? undefined} alt="" />
                  <AvatarFallback>
                    {(owner.displayName?.[0] ?? "C").toUpperCase()}
                  </AvatarFallback>
                </Avatar>
                <span className="text-sm font-medium">{owner.displayName}</span>
              </Link>
            )}
            {viewsLabel && (
              <span className="text-sm text-muted-foreground">{viewsLabel}</span>
            )}
            {video.status && video.status !== "READY" && (
              <span className="rounded-md bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
                {video.status}
              </span>
            )}
          </div>
          {video.description && (
            <p className="whitespace-pre-wrap text-sm leading-relaxed text-muted-foreground">
              {video.description}
            </p>
          )}
        </div>

        <section className="space-y-4 border-t border-border pt-5">
          <h2 className="text-lg font-semibold">Comments</h2>
          {isAuthenticated ? (
            <form onSubmit={submitComment} className="space-y-2">
              <Textarea
                value={commentBody}
                onChange={(e) => setCommentBody(e.target.value)}
                placeholder="Add a comment…"
                rows={3}
              />
              {commentError && (
                <p className="text-sm text-destructive">{commentError}</p>
              )}
              <Button type="submit" disabled={posting || !commentBody.trim()}>
                {posting ? "Posting…" : "Post comment"}
              </Button>
            </form>
          ) : (
            <p className="text-sm text-muted-foreground">
              <Link href="/login" className="text-primary underline-offset-4 hover:underline">
                Log in
              </Link>{" "}
              to leave a comment.
            </p>
          )}

          {comments.length === 0 ? (
            <EmptyState
              title="No comments yet"
              description="Start the conversation."
              className="py-10"
            />
          ) : (
            <ul className="space-y-4">
              {comments.map((c) => {
                const author = commentAuthor(c);
                return (
                  <li key={c.id} className="flex gap-3">
                    <Avatar className="size-8">
                      <AvatarImage src={author?.avatarUrl ?? undefined} alt="" />
                      <AvatarFallback>
                        {(author?.displayName?.[0] ?? "?").toUpperCase()}
                      </AvatarFallback>
                    </Avatar>
                    <div className="min-w-0 space-y-0.5">
                      <p className="text-sm font-medium">
                        {author?.displayName ?? "Viewer"}
                      </p>
                      <p className="text-sm text-foreground/90">
                        {commentText(c)}
                      </p>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </div>

      <aside className="space-y-4">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
          Related
        </h2>
        {related.length === 0 ? (
          <p className="text-sm text-muted-foreground">No related videos.</p>
        ) : (
          related.map((v) => <VideoCard key={v.id} video={v} compact />)
        )}
      </aside>
    </div>
  );
}
