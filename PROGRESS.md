# Clipzy — build progress

Track phase completion across agent sessions. Update after each phase; do not advance until acceptance criteria pass.

| Phase | Name | Status | Notes |
|-------|------|--------|-------|
| 0 | Environment & repo scaffold | **done** | Postgres host **5433**; MinIO via Quay |
| 1 | Backend skeleton | **done** | Boot 3.4.5 / Java 21 / **Gradle Groovy**; API **8081** |
| 2 | Data model & migrations | **done** | Flyway V2–V4; JPA entities + User IT |
| 3 | Auth (JWT) | **done** | register/login/refresh/me; jjwt 0.12.x |
| 4 | Upload & storage | **done** | MinIO/S3 presigned PUT; complete-upload → jobs |
| 5 | Transcoding worker | **done** | Scheduled FFmpeg HLS; `clipzy.ffmpeg.path` |
| 6 | Catalog & playback APIs | **done** | feed, search, views; public GET comments |
| 7 | Social features | **done** | comments, subscribe, watch history |
| 8 | Frontend scaffold | **done** | Next.js 16 App Router, Tailwind, shadcn |
| 9 | Video player | **done** | hls.js + custom controls |
| 10 | Real pages & upload flow | **done** | feed, watch, channel, auth, upload |
| 11 | Polish & QA | **done** | loading/empty states; vitest |
| 12 | Deployment | **done** | Dockerfiles, compose.prod, DEPLOY.md |
| 13 | Quality / settings menu | **done** | `useHlsQuality`; Auto + persist; speed submenu |
| 14 | Theme + i18n | **done** | next-themes; next-intl en/km/ko under `/[locale]` |

## Session log

- **2026-09-14** — Phases 13–14: YouTube-style quality/speed settings; dark/light/system theme; locales en/km/ko with Noto fallbacks.
