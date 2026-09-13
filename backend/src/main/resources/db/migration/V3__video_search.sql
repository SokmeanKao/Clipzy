-- Phase 6: full-text search on title + description

ALTER TABLE videos
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
            to_tsvector(
                'english',
                coalesce(title, '') || ' ' || coalesce(description, '')
            )
        ) STORED;

CREATE INDEX idx_videos_search_vector ON videos USING GIN (search_vector);
