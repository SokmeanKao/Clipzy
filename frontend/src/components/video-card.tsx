import { Link } from "@/i18n/navigation";
import type { Video } from "@/lib/types";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

function formatViews(n?: number) {
  if (n == null) return null;
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M views`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K views`;
  return `${n} views`;
}

export function VideoCard({
  video,
  compact = false,
  className,
}: {
  video: Video;
  compact?: boolean;
  className?: string;
}) {
  return (
    <Link
      href={`/watch/${video.id}`}
      className={cn(
        "group block focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
        compact ? "flex gap-3" : "space-y-2.5",
        className
      )}
    >
      <div
        className={cn(
          "relative overflow-hidden bg-muted",
          compact
            ? "aspect-video w-40 shrink-0 rounded-lg sm:w-44"
            : "aspect-video rounded-xl"
        )}
      >
        {video.thumbnailUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={video.thumbnailUrl}
            alt=""
            className="size-full object-cover transition duration-500 group-hover:scale-[1.03]"
          />
        ) : (
          <div className="flex size-full items-center justify-center bg-[linear-gradient(145deg,var(--muted),color-mix(in_oklch,var(--primary)_18%,var(--muted)))] text-xs font-medium text-muted-foreground">
            No thumbnail
          </div>
        )}
      </div>
      <div className="min-w-0 space-y-1">
        <h3
          className={cn(
            "font-medium leading-snug text-foreground transition-colors group-hover:text-primary",
            compact ? "line-clamp-2 text-sm" : "line-clamp-2 text-[0.95rem]"
          )}
        >
          {video.title}
        </h3>
        {video.owner?.displayName && (
          <p className="truncate text-sm text-muted-foreground">
            {video.owner.displayName}
          </p>
        )}
        {formatViews(video.viewCount) && (
          <p className="text-xs text-muted-foreground">
            {formatViews(video.viewCount)}
          </p>
        )}
      </div>
    </Link>
  );
}

export function VideoCardSkeleton({ compact = false }: { compact?: boolean }) {
  if (compact) {
    return (
      <div className="flex gap-3">
        <Skeleton className="aspect-video w-40 shrink-0 rounded-lg sm:w-44" />
        <div className="flex-1 space-y-2 pt-1">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-3 w-2/3" />
        </div>
      </div>
    );
  }
  return (
    <div className="space-y-2.5">
      <Skeleton className="aspect-video w-full rounded-xl" />
      <Skeleton className="h-4 w-5/6" />
      <Skeleton className="h-3 w-1/2" />
    </div>
  );
}
