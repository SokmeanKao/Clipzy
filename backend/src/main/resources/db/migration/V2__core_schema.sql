-- Phase 2: core Clipzy data model

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(320) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(120) NOT NULL,
    avatar_url      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE videos (
    id              UUID PRIMARY KEY,
    owner_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title           VARCHAR(300) NOT NULL,
    description     TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'uploading'
        CHECK (status IN ('uploading', 'processing', 'ready', 'failed')),
    manifest_path   TEXT,
    thumbnail_path  TEXT,
    view_count      BIGINT NOT NULL DEFAULT 0,
    visibility      VARCHAR(32) NOT NULL DEFAULT 'public'
        CHECK (visibility IN ('public', 'unlisted', 'private')),
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_videos_owner_id ON videos (owner_id);
CREATE INDEX idx_videos_status_visibility ON videos (status, visibility);
CREATE INDEX idx_videos_created_at ON videos (created_at DESC);

CREATE TABLE renditions (
    id              UUID PRIMARY KEY,
    video_id        UUID NOT NULL REFERENCES videos (id) ON DELETE CASCADE,
    height          INT NOT NULL,
    bitrate_kbps    INT NOT NULL,
    storage_path    TEXT NOT NULL
);

CREATE INDEX idx_renditions_video_id ON renditions (video_id);

CREATE TABLE subscriptions (
    subscriber_id   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    channel_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (subscriber_id, channel_id),
    CONSTRAINT chk_subscriptions_not_self CHECK (subscriber_id <> channel_id)
);

CREATE INDEX idx_subscriptions_channel_id ON subscriptions (channel_id);

CREATE TABLE comments (
    id              UUID PRIMARY KEY,
    video_id        UUID NOT NULL REFERENCES videos (id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    parent_id       UUID REFERENCES comments (id) ON DELETE CASCADE,
    body            TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_video_id ON comments (video_id);
CREATE INDEX idx_comments_parent_id ON comments (parent_id);

CREATE TABLE watch_history (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    video_id            UUID NOT NULL REFERENCES videos (id) ON DELETE CASCADE,
    progress_seconds    INT NOT NULL DEFAULT 0,
    last_watched_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_watch_history_user_video UNIQUE (user_id, video_id)
);

CREATE INDEX idx_watch_history_user_id ON watch_history (user_id, last_watched_at DESC);
