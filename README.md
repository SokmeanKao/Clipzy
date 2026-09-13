# Clipzy

Upload, transcode to HLS, and stream video with social features — comments, subscriptions, and watch history.

**Repository:** [github.com/SokmeanKao/Clipzy](https://github.com/SokmeanKao/Clipzy)

## Features

- Email/password auth (JWT access + refresh)
- Direct-to-object-storage uploads (presigned PUT)
- Background FFmpeg worker → multi-bitrate HLS (1080p / 720p / 480p)
- Catalog feed, full-text search, channels, views
- Comments, subscriptions, resume playback
- Custom hls.js player with YouTube-style quality + speed settings
- Light / dark / system theme
- i18n: English, Khmer (`km`), Korean (`ko`)

## Stack

| Layer | Tech |
|-------|------|
| Backend | Spring Boot 3.4, Java 21, **Gradle (Groovy)**, Flyway, JWT |
| Frontend | Next.js 16 (App Router), TypeScript, Tailwind, shadcn/ui, hls.js, next-intl, next-themes |
| Database | PostgreSQL 16 |
| Object storage | **MinIO** locally (S3-compatible); **AWS S3 + CDN** in production |
| Transcoding | FFmpeg → HLS master + renditions |

## Quick start (local)

```bash
# 1) Infra (Postgres on host port 5433, MinIO on 9000/9001)
docker compose -f infra/docker-compose.yml up -d

# 2) API
cd backend && ./gradlew bootRun
# → http://localhost:8081  (health: /actuator/health)

# 3) Web (PulseGrid or another app may already own :3000)
cd frontend && npm install && npm run dev -- -p 3001
# → http://localhost:3001/en
```

| Service | URL / connection |
|---------|------------------|
| App | http://localhost:3001/en (or `/km`, `/ko`) |
| API | http://localhost:8081 |
| Postgres | `localhost:5433` — `clipzy` / `clipzy` / `clipzy` |
| MinIO API | http://localhost:9000 |
| MinIO console | http://localhost:9001 — `minioadmin` / `minioadmin` |
| Bucket | `videos` (created by `minio-init`) |

FFmpeg must be on `PATH` (or set `FFMPEG_PATH`) when running the backend on the host.

## Full stack with Docker

```bash
docker compose -f infra/docker-compose.prod.yml up --build
```

See [DEPLOY.md](./DEPLOY.md) for production env vars (`S3_*`, Postgres, JWT, CORS) and switching MinIO → real S3.

## Repo layout

```
clipzy/
├── backend/                  # Spring Boot API + transcode worker
├── frontend/                 # Next.js app (routes under app/[locale]/…)
├── infra/
│   ├── docker-compose.yml        # Dev: Postgres + MinIO
│   └── docker-compose.prod.yml   # Full stack
├── DEPLOY.md
├── PROGRESS.md
└── README.md
```

## License

Private / unpublished unless otherwise stated by the repository owner.
