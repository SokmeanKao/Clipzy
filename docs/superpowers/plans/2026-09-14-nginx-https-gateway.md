# Nginx HTTPS Gateway Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose Clipzy on host port **443 only** via nginx (self-signed TLS), with frontend/backend as GHCR images only and `/api` + `/videos` path routing.

**Architecture:** nginx terminates TLS and reverse-proxies to frontend:3000, backend:8081 (under `/api/`), and MinIO (under `/videos/`). Backend S3 client stays on internal `http://minio:9000`; S3Presigner uses public endpoint `https://localhost` so browser uploads hit nginx. Frontend image is rebuilt with `NEXT_PUBLIC_API_URL=https://localhost/api`.

**Tech Stack:** nginx:alpine, OpenSSL self-signed certs, Docker Compose, GHCR images, Spring Boot AWS SDK S3Presigner, Next.js baked env

## Global Constraints

- Public host port: **443 only** (no 3000/8081/9000/9001/5433 published in pull compose)
- Frontend/backend: **image-only** (`ghcr.io/sokmeankao/clipzy-*`), no `build:` in `docker-compose.pull.yml`
- TLS: self-signed in `infra/nginx/certs/`; replaceable via volume mount
- Frontend bake: `NEXT_PUBLIC_API_URL=https://localhost/api`
- Backend CORS: `https://localhost`; `S3_PUBLIC_BASE_URL=https://localhost/videos`
- Spec: `docs/superpowers/specs/2026-09-14-nginx-https-gateway-design.md`

## File map

| File | Responsibility |
|------|----------------|
| `infra/nginx/nginx.conf` | TLS listener + proxy routes |
| `infra/nginx/docker-entrypoint.d/10-generate-certs.sh` | Create self-signed PEMs if missing |
| `infra/nginx/certs/.gitkeep` | Certs directory (PEMs gitignored) |
| `.gitignore` | Ignore `infra/nginx/certs/*.pem` |
| `docker-compose.pull.yml` | Pull stack + nginx gateway |
| `backend/.../ClipzyProperties.java` | Optional `publicEndpoint` for presigner |
| `backend/.../S3Config.java` | Presigner uses public endpoint |
| `backend/.../application.yml` | Bind `S3_PUBLIC_ENDPOINT` |
| `backend/src/test/.../S3PublicEndpointTest.java` | Unit test for endpoint resolution |
| `.github/workflows/docker-images.yml` | Frontend build-arg for `/api` |
| `README.md`, `DEPLOY.md` | Install via 443 |

---

### Task 1: Public S3 endpoint for browser presigns

**Files:**
- Modify: `backend/src/main/java/com/clipzy/config/ClipzyProperties.java`
- Modify: `backend/src/main/java/com/clipzy/config/S3Config.java`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/clipzy/config/S3PublicEndpointTest.java`

**Interfaces:**
- Consumes: existing `ClipzyProperties.S3` (`endpoint`, `publicBaseUrl`, credentials)
- Produces: `S3.resolvedPublicEndpoint()` → `String` URI used only by `S3Presigner` bean; `S3Client` still uses `getEndpoint()`

- [ ] **Step 1: Write failing unit test**

```java
package com.clipzy.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class S3PublicEndpointTest {

  @Test
  void resolvedPublicEndpoint_prefersExplicitPublicEndpoint() {
    ClipzyProperties.S3 s3 = new ClipzyProperties.S3();
    s3.setPublicEndpoint("https://cdn.example");
    s3.setPublicBaseUrl("https://localhost/videos");
    assertEquals("https://cdn.example", s3.resolvedPublicEndpoint());
  }

  @Test
  void resolvedPublicEndpoint_derivesFromPublicBaseUrl_strippingBucketPath() {
    ClipzyProperties.S3 s3 = new ClipzyProperties.S3();
    s3.setBucket("videos");
    s3.setPublicBaseUrl("https://localhost/videos");
    assertEquals("https://localhost", s3.resolvedPublicEndpoint());
  }

  @Test
  void resolvedPublicEndpoint_fallsBackToInternalEndpoint() {
    ClipzyProperties.S3 s3 = new ClipzyProperties.S3();
    s3.setEndpoint("http://minio:9000");
    s3.setPublicBaseUrl("");
    assertEquals("http://minio:9000", s3.resolvedPublicEndpoint());
  }
}
```

- [ ] **Step 2: Run test — expect FAIL**

Run (from `backend/`):

```bash
./gradlew test --tests com.clipzy.config.S3PublicEndpointTest
```

Expected: compile failure (`resolvedPublicEndpoint` / `publicEndpoint` missing)

- [ ] **Step 3: Implement property + resolution**

In `ClipzyProperties.S3` add:

```java
/** Browser-facing S3 API origin (scheme+host[+port]), no bucket path. */
private String publicEndpoint = "";

public String getPublicEndpoint() { return publicEndpoint; }
public void setPublicEndpoint(String publicEndpoint) {
  this.publicEndpoint = publicEndpoint;
}

/** Origin used when signing browser-facing URLs. */
public String resolvedPublicEndpoint() {
  if (publicEndpoint != null && !publicEndpoint.isBlank()) {
    return publicEndpoint.replaceAll("/$", "");
  }
  if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
    String base = publicBaseUrl.replaceAll("/$", "");
    String suffix = "/" + bucket;
    if (base.endsWith(suffix)) {
      return base.substring(0, base.length() - suffix.length());
    }
    return base;
  }
  return endpoint.replaceAll("/$", "");
}
```

In `application.yml` under `clipzy.s3`:

```yaml
public-endpoint: ${S3_PUBLIC_ENDPOINT:}
```

In `S3Config.s3Presigner`:

```java
.endpointOverride(URI.create(s3.resolvedPublicEndpoint()))
```

Leave `s3Client` on `s3.getEndpoint()`.

- [ ] **Step 4: Run test — expect PASS**

```bash
./gradlew test --tests com.clipzy.config.S3PublicEndpointTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/clipzy/config/ClipzyProperties.java \
  backend/src/main/java/com/clipzy/config/S3Config.java \
  backend/src/main/resources/application.yml \
  backend/src/test/java/com/clipzy/config/S3PublicEndpointTest.java
git commit -m "feat: sign S3 browser URLs with public endpoint for nginx"
```

---

### Task 2: Nginx TLS config + cert bootstrap

**Files:**
- Create: `infra/nginx/nginx.conf`
- Create: `infra/nginx/docker-entrypoint.d/10-generate-certs.sh`
- Create: `infra/nginx/certs/.gitkeep`
- Modify: `.gitignore` (add `infra/nginx/certs/*.pem`)

**Interfaces:**
- Consumes: upstream DNS names `frontend:3000`, `backend:8081`, `minio:9000`
- Produces: TLS on 443; routes `/api/`, `/videos/`, `/`

- [ ] **Step 1: Add gitignore for PEMs**

Append to `.gitignore`:

```
infra/nginx/certs/*.pem
```

Keep `infra/nginx/certs/.gitkeep`.

- [ ] **Step 2: Write `infra/nginx/nginx.conf`**

```nginx
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
  worker_connections 1024;
}

http {
  include /etc/nginx/mime.types;
  default_type application/octet-stream;
  sendfile on;
  keepalive_timeout 65;
  client_max_body_size 2g;

  map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
  }

  upstream clipzy_frontend {
    server frontend:3000;
  }

  upstream clipzy_backend {
    server backend:8081;
  }

  upstream clipzy_minio {
    server minio:9000;
  }

  server {
    listen 443 ssl;
    server_name localhost;

    ssl_certificate     /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    proxy_read_timeout 3600s;
    proxy_send_timeout 3600s;

    location /api/ {
      proxy_pass http://clipzy_backend/;
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /videos/ {
      proxy_pass http://clipzy_minio/videos/;
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
      proxy_buffering off;
    }

    location / {
      proxy_pass http://clipzy_frontend;
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection $connection_upgrade;
    }
  }
}
```

- [ ] **Step 3: Write cert generator script**

`infra/nginx/docker-entrypoint.d/10-generate-certs.sh`:

```sh
#!/bin/sh
set -eu
CERT_DIR=/etc/nginx/certs
mkdir -p "$CERT_DIR"
if [ ! -f "$CERT_DIR/fullchain.pem" ] || [ ! -f "$CERT_DIR/privkey.pem" ]; then
  echo "Generating self-signed TLS cert for localhost..."
  openssl req -x509 -nodes -newkey rsa:2048 -days 825 \
    -keyout "$CERT_DIR/privkey.pem" \
    -out "$CERT_DIR/fullchain.pem" \
    -subj "/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
fi
```

Make executable (`chmod +x`). Official `nginx` image runs scripts in `/docker-entrypoint.d/` — mount this file there.

- [ ] **Step 4: Commit**

```bash
git add infra/nginx .gitignore
git commit -m "feat: add nginx TLS gateway config and cert bootstrap"
```

---

### Task 3: Update `docker-compose.pull.yml` for gateway

**Files:**
- Modify: `docker-compose.pull.yml`

**Interfaces:**
- Consumes: nginx assets from Task 2; backend env contract from Task 1
- Produces: single published port `443:443`

- [ ] **Step 1: Rewrite pull compose**

Replace contents with a compose that:

1. Header comments document:

```text
curl -fsSL -o docker-compose.yml https://raw.githubusercontent.com/SokmeanKao/Clipzy/main/docker-compose.pull.yml
docker compose pull
docker compose up -d
# open https://localhost/en  (accept self-signed warning)
```

2. `postgres` / `minio`: **no** `ports:`
3. `minio` env add: `MINIO_SERVER_URL: https://localhost`
4. `backend`: **no** `ports:`; image only; env:

```yaml
S3_ENDPOINT: http://minio:9000
S3_PUBLIC_BASE_URL: https://localhost/videos
S3_PUBLIC_ENDPOINT: https://localhost
CLIPZY_CORS_ORIGINS: https://localhost
```

5. `frontend`: **no** `ports:`; image only; remove useless `NEXT_PUBLIC_API_URL` runtime env (baked in image)
6. Add `nginx`:

```yaml
nginx:
  image: nginx:1.27-alpine
  restart: unless-stopped
  depends_on:
    - frontend
    - backend
    - minio
  ports:
    - "443:443"
  volumes:
    - ./infra/nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    - ./infra/nginx/docker-entrypoint.d/10-generate-certs.sh:/docker-entrypoint.d/10-generate-certs.sh:ro
    - nginx_certs:/etc/nginx/certs
```

**Note for curl-only install:** volumes reference `./infra/nginx/...`, so **pull-only without clone cannot mount those files**. Fix in this task by either:

- **Required for curl path:** document that users must `git clone` (shallow) for nginx config, **or**
- Vendor nginx config into an image `ghcr.io/sokmeankao/clipzy-gateway` (extra CI image).

**Choose for this plan:** shallow clone is the install path for gateway (compose + `infra/nginx`). Update header to:

```bash
git clone --depth 1 https://github.com/SokmeanKao/Clipzy.git
cd Clipzy
docker compose -f docker-compose.pull.yml pull
docker compose -f docker-compose.pull.yml up -d
```

Keep `curl` of a single file **out of date** unless/until a gateway image exists — update README accordingly in Task 5.

Volumes section add `nginx_certs:`.

- [ ] **Step 2: Validate compose file**

```bash
docker compose -f docker-compose.pull.yml config
```

Expected: prints merged config; `ports` only under `nginx` → `443:443`

- [ ] **Step 3: Commit**

```bash
git add docker-compose.pull.yml
git commit -m "feat: route pull stack through nginx on 443 only"
```

---

### Task 4: CI frontend bake URL + local Dockerfile default

**Files:**
- Modify: `.github/workflows/docker-images.yml`
- Modify: `frontend/Dockerfile` (default ARG)
- Modify: `frontend/src/lib/types.test.ts` (contract string if it asserts `:8081`)

**Interfaces:**
- Produces: GHCR frontend with `NEXT_PUBLIC_API_URL=https://localhost/api`

- [ ] **Step 1: Update workflow build arg**

```yaml
build_args: "NEXT_PUBLIC_API_URL=https://localhost/api"
```

- [ ] **Step 2: Update Dockerfile default**

```dockerfile
ARG NEXT_PUBLIC_API_URL=https://localhost/api
```

- [ ] **Step 3: Fix frontend contract test**

In `frontend/src/lib/types.test.ts`, change expectation to document `https://localhost/api` (or assert `/api` suffix) instead of `:8081`.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/docker-images.yml frontend/Dockerfile frontend/src/lib/types.test.ts
git commit -m "chore: bake frontend API URL for nginx /api gateway"
```

---

### Task 5: Docs

**Files:**
- Modify: `README.md`
- Modify: `DEPLOY.md`

- [ ] **Step 1: Update install instructions**

Pull/gateway path:

```bash
git clone --depth 1 https://github.com/SokmeanKao/Clipzy.git
cd Clipzy
docker compose -f docker-compose.pull.yml pull
docker compose -f docker-compose.pull.yml up -d
```

App: `https://localhost/en` (accept cert warning).  
Replace certs: put PEMs in a bind mount over `nginx_certs` / documented volume path.  
GHCR auth / public packages note unchanged.

Remove claims that pull stack uses `:3000` / `:8081` / `:9000`.

Keep source-build path (`docker compose up --build`) as alternate without nginx unless already wired.

- [ ] **Step 2: Commit**

```bash
git add README.md DEPLOY.md
git commit -m "docs: install Clipzy via HTTPS gateway on port 443"
```

---

### Task 6: Local verification

**Files:** none (runtime)

- [ ] **Step 1: Build backend image locally (includes Task 1)**

```bash
docker compose -f docker-compose.pull.yml build --no-cache
```

Pull file has no build — so either temporarily use `infra/docker-compose.prod.yml` patterns, or:

```bash
docker build -t ghcr.io/sokmeankao/clipzy-backend:latest ./backend
docker build --build-arg NEXT_PUBLIC_API_URL=https://localhost/api -t ghcr.io/sokmeankao/clipzy-frontend:latest ./frontend
```

- [ ] **Step 2: Start pull stack**

```bash
docker compose -f docker-compose.pull.yml up -d
docker compose -f docker-compose.pull.yml ps
```

Expected: nginx published `0.0.0.0:443->443`; no host bindings on frontend/backend/postgres/minio.

- [ ] **Step 3: Smoke HTTPS**

```bash
curl.exe -k -s https://localhost/api/actuator/health
curl.exe -k -s -o NUL -w "%{http_code}" https://localhost/en
```

Expected: `{"status":"UP"}` and `200`.

- [ ] **Step 4: Push commits if not already pushed**

```bash
git push origin main
```

CI rebuilds GHCR images with new frontend bake + backend presign fix.

---

## Spec coverage check

| Spec item | Task |
|-----------|------|
| nginx :443 self-signed | 2, 3 |
| `/` `/api/` `/videos/` routes | 2 |
| FE/BE images only, no host ports | 3 |
| Postgres/MinIO unpublished | 3 |
| `NEXT_PUBLIC_API_URL=https://localhost/api` | 4 |
| CORS + S3 public URL | 3 |
| Presign via public host | 1, 3 |
| Docs | 5 |
| Smoke | 6 |
| No ACME / no :80 | respected (non-goals) |

## Placeholder scan

None intentional. Curl-only single-file install explicitly replaced by shallow clone (documented in Task 3/5).
