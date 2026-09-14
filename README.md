# Clipzy

Upload, transcode to HLS, and stream video with social features — comments, subscriptions, and watch history.

**Repository:** [github.com/SokmeanKao/Clipzy](https://github.com/SokmeanKao/Clipzy)

## Run with Docker (HTTPS gateway on 443)

Shallow clone (needs `infra/nginx` configs), pull app images, expose **only port 443**:

```bash
git clone --depth 1 https://github.com/SokmeanKao/Clipzy.git
cd Clipzy
docker compose -f docker-compose.pull.yml pull
docker compose -f docker-compose.pull.yml up -d
```

| Service | URL |
|---------|-----|
| App | https://localhost/en (accept self-signed cert warning) |
| API health | https://localhost/api/actuator/health |

Stop: `docker compose -f docker-compose.pull.yml down`

Replace TLS later: put `fullchain.pem` + `privkey.pem` into the `nginx_certs` Docker volume.

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

Builds backend/frontend locally (no nginx gateway; ports 3000/8081).

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
