# Clipzy deployment

## Run on any PC (pull images + nginx on 443)

```bash
git clone --depth 1 https://github.com/SokmeanKao/Clipzy.git
cd Clipzy
docker compose -f docker-compose.pull.yml pull
docker compose -f docker-compose.pull.yml up -d
```

| Piece | Role |
|-------|------|
| `nginx:1.27-alpine` | TLS gateway — **only host port 443** |
| `ghcr.io/sokmeankao/clipzy-backend` | API + FFmpeg worker (internal) |
| `ghcr.io/sokmeankao/clipzy-frontend` | Next.js UI (internal) |
| `postgres:16` | Database (internal) |
| `quay.io/minio/minio` | Object storage (internal) |

Routes: `/` → frontend, `/api/` → backend, `/videos/` → MinIO.  
App: https://localhost/en (self-signed warning is expected).  
Replace certs: write PEMs into the `nginx_certs` volume.

CI pushes Clipzy images on every push to `main` and on tags (`v*`). Frontend bake: `NEXT_PUBLIC_API_URL=https://localhost/api`. See `.github/workflows/docker-images.yml`.

### Make GHCR packages public (one-time)

1. https://github.com/SokmeanKao/Clipzy/pkgs/container/clipzy-backend → Package settings → **Public**
2. Repeat for `clipzy-frontend`

Until then: `docker login ghcr.io`.

Pin a release: `CLIPZY_TAG=v1.0.0 docker compose -f docker-compose.pull.yml up -d`

## Alternative: git clone → build from source

```bash
git clone https://github.com/SokmeanKao/Clipzy.git
cd Clipzy
docker compose up --build -d
```

Root `docker-compose.yml` builds local images (no nginx). Same stack alternate: `infra/docker-compose.prod.yml`.

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
