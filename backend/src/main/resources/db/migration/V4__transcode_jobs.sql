-- Phase 4: async transcoding job queue

CREATE TABLE transcode_jobs (
    id              UUID PRIMARY KEY,
    video_id        UUID NOT NULL REFERENCES videos (id) ON DELETE CASCADE,
    status          VARCHAR(32) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'running', 'succeeded', 'failed')),
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transcode_jobs_status_created ON transcode_jobs (status, created_at);
CREATE INDEX idx_transcode_jobs_video_id ON transcode_jobs (video_id);
