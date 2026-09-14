# Clipzy deployment

## Run on any PC (published images)

From a clone of this repo:

```bash
docker compose up -d
```

This pulls:

| Image | Role |
|-------|------|
| `ghcr.io/sokmeankao/clipzy-backend` | API + FFmpeg worker (**our** image) |
| `ghcr.io/sokmeankao/clipzy-frontend` | Next.js UI (**our** image) |
| `postgres:16` | Database (official) |
| `quay.io/minio/minio` | Object storage (official) |

CI builds and pushes the two Clipzy images on every push to `main` and on version tags (`v*`). See `.github/workflows/docker-images.yml`.

### Make GHCR packages public (one-time)

1. Open https://github.com/SokmeanKao/Clipzy/pkgs/container/clipzy-backend
2. Package settings → Change visibility → **Public**
3. Repeat for `clipzy-frontend`

Until then, consumers need `docker login ghcr.io`.

## Build from source (local prod compose)

```bash
docker compose -f infra/docker-compose.prod.yml up --build
```

| Service | Host URL |
|---------|----------|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8081 |
| Health | http://localhost:8081/actuator/health |
| MinIO API | http://localhost:9000 |
| MinIO console | http://localhost:9001 (`minioadmin` / `minioadmin`) |
| Postgres | `localhost:5433` (user/pass/db `clipzy`) |

Stop:

```bash
docker compose -f infra/docker-compose.prod.yml down
# or, for the root compose file:
docker compose down
```

### Local ports note (5433 vs 5432)

- **Inside Compose**, Postgres listens on **5432** (`DB_URL=jdbc:postgresql://postgres:5432/clipzy`).
- **On the host**, Postgres is published as **5433→5432** so it does not clash with other local Postgres instances.
- Do **not** run `infra/docker-compose.yml` and the full-stack compose files at the same time — they share ports 5433 / 9000 / 9001.

## Images

| Path | Role |
|------|------|
| `backend/Dockerfile` | Multi-stage Gradle (Temurin 21) + JRE **with FFmpeg** → `clipzy-backend` |
| `frontend/Dockerfile` | Next.js standalone → `clipzy-frontend` (`NEXT_PUBLIC_API_URL` build-arg) |

MinIO images use **quay.io** because Docker Hub may deny anonymous pulls.

## Required env vars (real production)

Replace MinIO / local defaults with managed services:

### Database (managed Postgres)

| Variable | Example |
|----------|---------|
| `DB_URL` | `jdbc:postgresql://your-db.example:5432/clipzy` |
| `DB_USER` | app user |
| `DB_PASSWORD` | strong password |

### Object storage & CDN

| Variable | Description |
|----------|-------------|
| `S3_ENDPOINT` | S3 API endpoint (AWS regional or compatible) |
| `S3_ACCESS_KEY` | Access key / IAM key |
| `S3_SECRET_KEY` | Secret key |
| `S3_BUCKET` | Bucket name (e.g. `videos`) |
| `S3_REGION` | e.g. `us-east-1` |
| `S3_PUBLIC_BASE_URL` | Browser-reachable base for HLS (CDN or public bucket URL) |

### Auth & CORS

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | Long random secret (≥32 bytes for HS256). **Never** use the compose default in production. |
| `CLIPZY_CORS_ORIGINS` | Comma-separated allowed origins, e.g. `https://app.example.com` |

### Frontend build-time API URL

`NEXT_PUBLIC_API_URL` is **baked into the frontend image at build time**. The CI image uses `http://localhost:8081` for local Docker users. For a custom domain, rebuild:

```bash
docker build -t my-clipzy-ui ./frontend --build-arg NEXT_PUBLIC_API_URL=https://api.example.com
```
