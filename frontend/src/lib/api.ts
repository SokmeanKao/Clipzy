import type {
  AuthResponse,
  Channel,
  Comment,
  CreateVideoResponse,
  PageResponse,
  User,
  Video,
  WatchHistoryItem,
} from "./types";
import { normalizeVideo, normalizeVideos } from "./types";

const ACCESS_KEY = "clipzy_access_token";
const REFRESH_KEY = "clipzy_refresh_token";
const USER_KEY = "clipzy_user";

export const API_BASE =
  process.env.NEXT_PUBLIC_API_URL?.replace(/\/$/, "") ||
  "http://localhost:8081";

export class ApiError extends Error {
  status: number;
  body: unknown;

  constructor(message: string, status: number, body?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(ACCESS_KEY);
}

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(REFRESH_KEY);
}

export function getStoredUser(): User | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}

export function setAuth(tokens: {
  accessToken: string;
  refreshToken: string;
  user?: User;
}) {
  localStorage.setItem(ACCESS_KEY, tokens.accessToken);
  localStorage.setItem(REFRESH_KEY, tokens.refreshToken);
  if (tokens.user) {
    localStorage.setItem(USER_KEY, JSON.stringify(tokens.user));
  }
}

export function clearAuth() {
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
  localStorage.removeItem(USER_KEY);
}

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
  auth?: boolean;
  skipRefresh?: boolean;
};

let refreshPromise: Promise<boolean> | null = null;

async function refreshAccessToken(): Promise<boolean> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return false;

  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) {
      clearAuth();
      return false;
    }
    const data = (await res.json()) as AuthResponse & {
      access_token?: string;
      refresh_token?: string;
    };
    const accessToken = data.accessToken ?? data.access_token;
    const nextRefresh = data.refreshToken ?? data.refresh_token ?? refreshToken;
    if (!accessToken) {
      clearAuth();
      return false;
    }
    setAuth({
      accessToken,
      refreshToken: nextRefresh,
      user: data.user ?? getStoredUser() ?? undefined,
    });
    return true;
  } catch {
    clearAuth();
    return false;
  }
}

export async function apiFetch<T = unknown>(
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  const { body, auth = true, skipRefresh = false, headers, ...rest } = options;
  const url = path.startsWith("http") ? path : `${API_BASE}${path}`;

  const finalHeaders = new Headers(headers);
  if (body !== undefined && !(body instanceof FormData)) {
    finalHeaders.set("Content-Type", "application/json");
  }
  if (auth) {
    const token = getAccessToken();
    if (token) finalHeaders.set("Authorization", `Bearer ${token}`);
  }

  const res = await fetch(url, {
    ...rest,
    headers: finalHeaders,
    body:
      body === undefined
        ? undefined
        : body instanceof FormData
          ? body
          : JSON.stringify(body),
  });

  if (res.status === 401 && auth && !skipRefresh) {
    if (!refreshPromise) {
      refreshPromise = refreshAccessToken().finally(() => {
        refreshPromise = null;
      });
    }
    const ok = await refreshPromise;
    if (ok) {
      return apiFetch<T>(path, { ...options, skipRefresh: true });
    }
  }

  if (res.status === 204) {
    return undefined as T;
  }

  const text = await res.text();
  let data: unknown = undefined;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!res.ok) {
    const message =
      typeof data === "object" &&
      data &&
      "message" in data &&
      typeof (data as { message: unknown }).message === "string"
        ? (data as { message: string }).message
        : `Request failed (${res.status})`;
    throw new ApiError(message, res.status, data);
  }

  return data as T;
}

export const api = {
  register(payload: {
    email: string;
    password: string;
    displayName: string;
  }) {
    return apiFetch<AuthResponse>("/auth/register", {
      method: "POST",
      body: payload,
      auth: false,
    });
  },

  login(payload: { email: string; password: string }) {
    return apiFetch<AuthResponse>("/auth/login", {
      method: "POST",
      body: payload,
      auth: false,
    });
  },

  me() {
    return apiFetch<User>("/me");
  },

  videos(page = 0, size = 24) {
    return apiFetch<PageResponse<Video> | Video[]>(
      `/videos?page=${page}&size=${size}`,
      { auth: false }
    ).then((data) => ({ content: normalizeVideos(data) }));
  },

  video(id: string) {
    return apiFetch<Video>(`/videos/${id}`, { auth: false }).then(normalizeVideo);
  },

  search(q: string, page = 0, size = 24) {
    return apiFetch<PageResponse<Video> | Video[]>(
      `/videos/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}`,
      { auth: false }
    ).then((data) => ({ content: normalizeVideos(data) }));
  },

  createVideo(payload: {
    title: string;
    description?: string;
    visibility?: string;
  }) {
    return apiFetch<CreateVideoResponse>("/videos", {
      method: "POST",
      body: payload,
    });
  },

  completeUpload(id: string) {
    return apiFetch<void>(`/videos/${id}/complete-upload`, { method: "POST" });
  },

  recordView(id: string) {
    return apiFetch<void>(`/videos/${id}/view`, {
      method: "POST",
      auth: false,
    });
  },

  reportProgress(id: string, progressSeconds: number) {
    return apiFetch<void>(`/videos/${id}/progress`, {
      method: "POST",
      body: { progressSeconds },
    });
  },

  comments(id: string) {
    return apiFetch<PageResponse<Comment> | Comment[]>(
      `/videos/${id}/comments`,
      { auth: false }
    );
  },

  postComment(id: string, body: string) {
    return apiFetch<Comment>(`/videos/${id}/comments`, {
      method: "POST",
      body: { body, content: body, text: body },
    });
  },

  channel(userId: string) {
    return apiFetch<Channel>(`/channels/${userId}`, { auth: false });
  },

  subscribe(channelId: string) {
    return apiFetch<void>(`/channels/${channelId}/subscribe`, {
      method: "POST",
    });
  },

  unsubscribe(channelId: string) {
    return apiFetch<void>(`/channels/${channelId}/subscribe`, {
      method: "DELETE",
    });
  },

  history() {
    return apiFetch<PageResponse<WatchHistoryItem> | WatchHistoryItem[]>(
      "/me/history"
    );
  },
};

export async function putFileWithProgress(
  uploadUrl: string,
  file: File,
  onProgress?: (pct: number) => void
): Promise<void> {
  await new Promise<void>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("PUT", uploadUrl);
    xhr.setRequestHeader(
      "Content-Type",
      file.type || "application/octet-stream"
    );
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable && onProgress) {
        onProgress(Math.round((e.loaded / e.total) * 100));
      }
    };
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) resolve();
      else reject(new ApiError(`Upload failed (${xhr.status})`, xhr.status));
    };
    xhr.onerror = () => reject(new ApiError("Upload network error", 0));
    xhr.send(file);
  });
}
