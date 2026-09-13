# Clipzy frontend QA

## Run locally

```bash
cd frontend
cp .env.example .env.local   # optional; defaults to http://localhost:8081
npm install
npm run dev
```

Open http://localhost:3000

Backend should be running on http://localhost:8081 for live data.

## Automated smoke

```bash
cd frontend
npm run test
```

Vitest covers `unwrapList` / comment helpers and API base URL defaults.

## Manual checklist

- [ ] Home shows hero **Clipzy** + feed (empty state OK if API has no videos)
- [ ] Search box submits without console errors
- [ ] Register → tokens in `localStorage` (`clipzy_access_token`, `clipzy_refresh_token`)
- [ ] Login works; header shows avatar menu
- [ ] `/upload` redirects to `/login?next=/upload` when logged out
- [ ] Upload: create → PUT uploadUrl with progress → complete-upload → poll until READY/FAILED
- [ ] `/watch/[id]`: custom player (no native controls), play/seek/volume/speed/quality
- [ ] Progress POSTs ~every 10s while playing (Network tab)
- [ ] Resume: watch partly, reload, seek near previous position when history exists
- [ ] Comments: list + post when authenticated
- [ ] Related sidebar shows other feed items
- [ ] `/channel/[id]`: profile, subscribe/unsubscribe when logged in
- [ ] Mobile: header wraps, watch layout stacks, player controls usable
- [ ] Error boundary: temporary throw recovers via Try again
- [ ] `npm run build` succeeds
