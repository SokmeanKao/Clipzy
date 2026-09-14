# Clipzy

Upload, transcode to HLS, and stream video with social features — comments, subscriptions, and watch history.

**Repository:** [github.com/SokmeanKao/Clipzy](https://github.com/SokmeanKao/Clipzy)

## Run with Docker (pull compose + images)

No git clone needed — download Compose and pull published images:

```bash
mkdir clipzy && cd clipzy
curl -fsSL -o docker-compose.yml https://raw.githubusercontent.com/SokmeanKao/Clipzy/main/docker-compose.pull.yml
docker compose pull
docker compose up -d
```

| Service | URL |
|---------|-----|
| App | http://localhost:3000/en |
| API health | http://localhost:8081/actuator/health |
| MinIO console | http://localhost:9001 (`minioadmin` / `minioadmin`) |

Stop: `docker compose down`

If image pull says `unauthorized`, make GHCR packages **Public** (GitHub → Packages → `clipzy-backend` / `clipzy-frontend` → Package settings), or:

```bash
echo YOUR_GITHUB_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
```

### Alternative: git clone → build from source

```bash
git clone https://github.com/SokmeanKao/Clipzy.git
cd Clipzy
docker compose up --build -d
```

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
