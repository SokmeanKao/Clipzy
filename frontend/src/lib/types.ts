export type User = {
  id: string;
  email?: string;
  displayName: string;
  avatarUrl?: string | null;
};

export type AuthTokens = {
  accessToken: string;
  refreshToken: string;
};

export type AuthResponse = AuthTokens & {
  user: User;
};

export type VideoOwner = {
  id: string;
  displayName: string;
  avatarUrl?: string | null;
};

export type VideoStatus =
  | "PENDING_UPLOAD"
  | "UPLOADING"
  | "PROCESSING"
  | "READY"
  | "FAILED"
  | string;

export type Video = {
  id: string;
  title: string;
  description?: string | null;
  status: VideoStatus;
  manifestUrl?: string | null;
  thumbnailUrl?: string | null;
  thumbnailPath?: string | null;
  viewCount?: number;
  visibility?: string;
  owner?: VideoOwner | null;
  ownerId?: string;
  ownerDisplayName?: string | null;
  createdAt?: string;
  durationSeconds?: number | null;
  progressSeconds?: number | null;
};

export type PageResponse<T> = {
  content?: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
  page?: number;
  items?: T[];
};

export type Comment = {
  id: string;
  body: string;
  content?: string;
  text?: string;
  createdAt?: string;
  author?: User | VideoOwner | null;
  user?: User | VideoOwner | null;
};

export type Channel = {
  id: string;
  userId?: string;
  displayName: string;
  avatarUrl?: string | null;
  bio?: string | null;
  description?: string | null;
  subscriberCount?: number;
  subscribed?: boolean;
  isSubscribed?: boolean;
  videos?: Video[];
};

export type WatchHistoryItem = {
  videoId?: string;
  video?: Video;
  progressSeconds?: number;
  watchedSeconds?: number;
  positionSeconds?: number;
};

export type CreateVideoResponse = {
  id: string;
  uploadUrl: string;
};

export function unwrapList<T>(data: PageResponse<T> | T[] | null | undefined): T[] {
  if (!data) return [];
  if (Array.isArray(data)) return data;
  if (Array.isArray(data.content)) return data.content;
  if (Array.isArray(data.items)) return data.items;
  return [];
}

/** Map flat backend VideoResponse fields into the shape the UI expects. */
export function normalizeVideo(v: Video): Video {
  const owner =
    v.owner ??
    (v.ownerId
      ? {
          id: v.ownerId,
          displayName: v.ownerDisplayName ?? "Channel",
          avatarUrl: null,
        }
      : null);
  return {
    ...v,
    owner,
    thumbnailUrl: v.thumbnailUrl ?? v.thumbnailPath ?? null,
    status: typeof v.status === "string" ? v.status.toUpperCase() : v.status,
  };
}

export function normalizeVideos(
  data: PageResponse<Video> | Video[] | null | undefined
): Video[] {
  return unwrapList(data).map(normalizeVideo);
}

export function commentText(c: Comment): string {
  return c.body ?? c.content ?? c.text ?? "";
}

export function commentAuthor(c: Comment): User | VideoOwner | null | undefined {
  return c.author ?? c.user;
}
