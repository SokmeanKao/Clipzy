# Clipzy deployment

Production-oriented Docker setup for the full stack (API, Next.js, Postgres, object storage).

## Quick start (local prod compose)

From the **repo root**:

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
```

### Local ports note (5433 vs 5432)

- **Inside Compose**, Postgres listens on **5432** (`DB_URL=jdbc:postgresql://postgres:5432/clipzy`).
- **On the host**, Postgres is published as **5433→5432** so it does not clash with other local Postgres instances (same convention as `infra/docker-compose.yml` for local/dev).
- Local/dev backend defaults use `jdbc:postgresql://localhost:5433/clipzy` when running outside Docker.

Do **not** run `infra/docker-compose.yml` and `infra/docker-compose.prod.yml` at the same time — both publish 5433, 9000, and 9001.

Dev-only infra (Postgres + MinIO, no app images) remains:

```bash
docker compose -f infra/docker-compose.yml up -d
```

## Images

| Path | Role |
|------|------|
| `backend/Dockerfile` | Multi-stage Gradle (Temurin 21 JDK) + JRE runtime **with FFmpeg** |
| `frontend/Dockerfile` | Multi-stage Next.js 16 (`output: 'standalone'`) on Node 22 Alpine |

MinIO images use **quay.io** (`quay.io/minio/minio`, `quay.io/minio/mc`) because Docker Hub may deny anonymous pulls.

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
| `CLIPZY_CORS_ORIGINS` | Comma-separated allowed origins, e.g. `https://app.example.com,https://www.example.com` |

### Transcoding

| Variable | Description |
|----------|-------------|
| `FFMPEG_PATH` | Path or name of FFmpeg (`ffmpeg` when installed in the image) |

### Frontend build

| Variable | Description |
|----------|-------------|
| `NEXT_PUBLIC_API_URL` | Public API origin baked into the Next.js build (e.g. `https://api.example.com`) |

Pass it as a Docker build arg:

```bash
docker build -t clipzy-frontend \
  --build-arg NEXT_PUBLIC_API_URL=https://api.example.com \
  ./frontend
```

## Replacing MinIO with AWS S3

1. Create an S3 bucket (e.g. `clipzy-videos`) and IAM credentials with PutObject / GetObject (and List if needed).
2. Optionally put a CloudFront (or other CDN) distribution in front of the bucket for playback.
3. Remove or stop the `minio` / `minio-init` services from your deploy stack.
4. Set backend env, for example:

```env
S3_ENDPOINT=https://s3.us-east-1.amazonaws.com
S3_ACCESS_KEY=AKIA...
S3_SECRET_KEY=...
S3_BUCKET=clipzy-videos
S3_REGION=us-east-1
S3_PUBLIC_BASE_URL=https://d111111abcdef8.cloudfront.net
```

Notes:

- The API uses path-style access compatible with MinIO locally; against AWS, ensure the SDK config matches your account (endpoint + region).
- `S3_PUBLIC_BASE_URL` must be what **browsers** use to fetch manifests and segments (CDN or public object URLs), not an internal Docker hostname.
- Presigned upload URLs are generated with `S3_ENDPOINT` credentials; clients upload directly to S3.

## Backend image only

```bash
docker build -t clipzy-backend ./backend
docker run --rm -p 8081:8081 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5433/clipzy \
  -e S3_ENDPOINT=http://host.docker.internal:9000 \
  -e S3_PUBLIC_BASE_URL=http://localhost:9000/videos \
  -e JWT_SECRET=your-long-secret-at-least-32-characters!! \
  -e CLIPZY_CORS_ORIGINS=http://localhost:3000 \
  -e FFMPEG_PATH=ffmpeg \
  clipzy-backend
```

## Checklist before production

- [ ] Strong unique `JWT_SECRET`
- [ ] Managed Postgres with backups
- [ ] Real S3 (or compatible) + CDN public base URL
- [ ] `CLIPZY_CORS_ORIGINS` limited to your real web origins
- [ ] `NEXT_PUBLIC_API_URL` points at the public API
- [ ] TLS termination (reverse proxy / load balancer) in front of frontend and API
