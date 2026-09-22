# Wavelength frontend

Next.js App Router frontend for the Wavelength real-time messenger.

## Run it

```bash
npm install --omit=optional --maxsockets=1
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

The `--maxsockets=1` option is intentional: it avoids timeouts on slow or unstable networks while downloading Next's platform binary.

## Environment

Copy `.env.example` to `.env.local`, then set the Spring API and WebSocket origins:

```bash
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
```

`lib/api/client.ts` is the typed REST boundary and `lib/ws/client.ts` provides the reconnecting WebSocket event bus. Wire these into your authentication and TanStack Query/Zustand layers once the backend route/event contracts are available.

## Key routes

- `/` — marketing landing page
- `/login`, `/register`, `/verify`, `/forgot-password` — authentication flows
- `/chats` — responsive messenger workspace
- `/groups`, `/channels`, `/contacts`, `/saved` — collaboration surfaces
- `/settings/account`, `/settings/privacy`, `/settings/notifications`, `/settings/sessions`, `/settings/appearance`, `/settings/language`, `/settings/security` — settings, including Theme Studio
