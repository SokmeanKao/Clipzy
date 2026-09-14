# Clipzy

Upload, transcode to HLS, and stream video with social features — comments, subscriptions, and watch history.

**Repository:** [github.com/SokmeanKao/Clipzy](https://github.com/SokmeanKao/Clipzy)

## Run with Docker (recommended for others)

Pulls **only our app images** (backend + UI) from GitHub Container Registry. Postgres and MinIO use official images.

```bash
git clone https://github.com/SokmeanKao/Clipzy.git
cd Clipzy
docker compose up -d
```

| Service | URL |
|---------|-----|
| App | http://localhost:3000/en |
| API health | http://localhost:8081/actuator/health |
| MinIO console | http://localhost:9001 (`minioadmin` / `minioadmin`) |

Stop: `docker compose down`

Published images:

- `ghcr.io/sokmeankao/clipzy-backend:latest`
- `ghcr.io/sokmeankao/clipzy-frontend:latest`

If pulls fail with `unauthorized`, either make the packages **Public** on GitHub (Packages → package → Package settings → Change visibility), or:

```bash
echo YOUR_GITHUB_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
```

Pin a release: `CLIPZY_TAG=v1.0.0 docker compose up -d`

Build from source instead of pulling: `docker compose -f infra/docker-compose.prod.yml up --build`

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
| Backend | Spring Boot 3.4, Java 21, Gradle (Groovy), Flyway, JWT |
| Frontend | Next.js 16 (App Router), TypeScript, Tailwind, shadcn/ui, hls.js, next-intl |
| Database | PostgreSQL 16 |
| Object storage | MinIO locally (S3-compatible); AWS S3 + CDN in production |
| Transcoding | FFmpeg → HLS master + renditions |
| CI images | GitHub Actions → GHCR (`clipzy-backend`, `clipzy-frontend`) |

## Development (host processes)

```bash
# Infra only
docker compose -f infra/docker-compose.yml up -d

# API
cd backend && ./gradlew bootRun
# → http://localhost:8081

# Web
cd frontend && npm install && npm run dev -- -p 3001
# → http://localhost:3001/en
```

FFmpeg must be on `PATH` (or set `FFMPEG_PATH`) when running the backend on the host.

See [DEPLOY.md](./DEPLOY.md) for production env vars and S3 cutover.

## Repo layout

```
clipzy/
├── docker-compose.yml            # Run with published GHCR images
├── .github/workflows/            # Build/push backend + frontend images
├── backend/
├── frontend/
├── infra/
│   ├── docker-compose.yml        # Dev: Postgres + MinIO only
│   └── docker-compose.prod.yml   # Build stack from source
├── DEPLOY.md
└── README.md
```

## License

Private / unpublished unless otherwise stated by the repository owner.
