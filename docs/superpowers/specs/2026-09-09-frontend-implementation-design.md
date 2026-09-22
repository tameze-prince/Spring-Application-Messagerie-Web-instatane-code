# Frontend Implementation Design — Wavelength Messenger

**Date:** 2026-09-09
**Status:** Approved for Implementation
**PRD Version:** 1.0 (Frontend) + 1.0 (Backend)
**Approach:** Incremental PRD-First (Approach A)

---

## 1. Architecture

### 1.1 High-Level Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    Next.js App Router                        │
├─────────────────────────────────────────────────────────────┤
│  (marketing)   (auth)          (app)          (admin)       │
│  /page.tsx     /login          /chats         /(admin)/     │
│                /register       /groups        users/        │
│                /verify         /channels      groups/       │
│                /forgot         /contacts      channels/     │
│                              /settings      messages/       │
│                              /saved         reports/        │
└─────────────────────────────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        ▼                  ▼                  ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│  Zustand      │ │  TanStack     │ │  Native       │
│  Stores       │ │  Query        │ │  WebSocket    │
│  (UI State)   │ │  (Server      │ │  Client       │
│               │ │   Cache)      │ │  (Real-time)  │
└───────────────┘ └───────────────┘ └───────────────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           ▼
              ┌───────────────────────┐
              │   Spring Boot API     │
              │   + STOMP WebSocket   │
              └───────────────────────┘
```

### 1.2 State Ownership

| Store | Owns | Persists |
|-------|------|----------|
| `useAuthStore` | user, tokens, session, auth status | localStorage (tokens) |
| `useConversationStore` | conversations list, activeConversationId, Pulse Rail data | — |
| `useMessageStore` | messages by conversationId, optimistic sends, drafts | localStorage (drafts) |
| `useThemeStore` | activeFrequency, density, motion, customFrequencies | localStorage + backend sync |
| `useUIStore` | commandPaletteOpen, detailsPanelOpen, toasts, splitView | — |

### 1.3 API Layer

`lib/api/hooks.ts` exports typed hooks using TanStack Query with cursor-based pagination matching backend:

- `useConversations()` — list with last message preview
- `useMessages(conversationId, cursor?)` — paginated messages
- `useUserSearch(query)` — debounced username/email/name search
- `useGroups()`, `useChannels()`, `useContacts()` — list hooks
- `useSettings(section)` — per-section settings mutations
- `useFileUpload()` — presigned URL flow

### 1.4 WebSocket Protocol (Native, not STOMP)

**Client → Server:**
```typescript
{ type: "message.send", requestId: "uuid", conversationId, clientMessageId, type, body, replyToId? }
{ type: "typing.start", conversationId }
{ type: "typing.stop", conversationId }
{ type: "conversation.read", conversationId, messageId }
{ type: "reaction.add", messageId, reaction }
{ type: "reaction.remove", messageId, reaction }
{ type: "presence.update", status }
```

**Server → Client:**
```typescript
{ type: "message.created", requestId, message }
{ type: "message.updated", message }
{ type: "message.deleted", messageId, forEveryone }
{ type: "message.delivered", messageId, userId }
{ type: "message.read", messageId, userId }
{ type: "typing.started", conversationId, userId, username }
{ type: "typing.stopped", conversationId, userId }
{ type: "presence.changed", userId, status, lastSeenAt? }
{ type: "reaction.added", messageId, userId, reaction }
{ type: "reaction.removed", messageId, userId, reaction }
{ type: "conversation.updated", conversation }
{ type: "notification.created", notification }
```

---

## 2. Data Flow

### 2.1 Message Send (Optimistic → Reconciled)

```
User types → Composer.onSubmit()
    │
    ▼
createOptimisticMessage({ clientMessageId, status: "SENDING" })
    │
    ▼
useMessageStore.addMessage(optimisticMessage)
    │
    ▼
WS.send({ type: "message.send", clientMessageId, ... })
    │
    ▼
[Server processes, persists, broadcasts]
    │
    ▼
WS.on("message.created") → reconcile(clientMessageId, serverMessage)
    │         (swap optimistic for confirmed in-place)
    ▼
Update message status: SENDING → SENT → DELIVERED → READ
```

### 2.2 Reconnection & Reconciliation

```
WS.onclose → scheduleReconnect(backoff)
    │
    ▼
WS.onopen → sendPendingMessages() (unsent clientMessageIds)
    │
    ▼
Server returns missed events (via conversation.updated + message.*)
    │
    ▼
Client applies events in sequence order → UI catches up
```

### 2.3 Theme Switch (Instant, No Reload)

```
FrequencySwitcher.onChange("dusk")
    │
    ▼
useThemeStore.setFrequency("dusk")
    │
    ▼
document.documentElement.dataset.frequency = "dusk"
    │
    ▼
CSS vars update → all components react via Tailwind utilities
    │
    ▼
Persist to localStorage + debounced sync to backend /users/me
```

---

## 3. Component Hierarchy (PRD §10.1 Structure)

```
app/
├── (marketing)/
│   ├── page.tsx          → Landing (Server Component)
│   └── layout.tsx
├── (auth)/
│   ├── login/page.tsx    → AuthScreen(mode="login")
│   ├── register/page.tsx
│   ├── verify/page.tsx
│   └── forgot-password/page.tsx
└── (app)/
    ├── layout.tsx        → AppShell (Provider wrappers)
    ├── chats/
    │   └── page.tsx      → Messenger (Client Component)
    ├── groups/page.tsx   → GroupsPage
    ├── channels/page.tsx → ChannelsPage
    ├── contacts/page.tsx → ContactsPage
    ├── saved/page.tsx    → SavedPage
    └── settings/
        ├── layout.tsx    → SettingsShell (side nav + content)
        ├── account/page.tsx
        ├── privacy/page.tsx
        ├── notifications/page.tsx
        ├── sessions/page.tsx
        ├── appearance/page.tsx  → ThemeStudio
        ├── language/page.tsx
        └── security/page.tsx

components/
├── ui/                   → shadcn/ui components (Button, Card, Dialog, etc.)
├── chat/
│   ├── MessageList.tsx           → @tanstack/react-virtual
│   ├── MessageBubble.tsx         → status badges, reply preview, reactions
│   ├── Composer.tsx              → undo-send, drafts, media quick-edit
│   ├── TypingPulse.tsx           → Pulse motif
│   ├── PulseRail.tsx             → activity visualization
│   ├── ConversationList.tsx      → virtualized, searchable
│   └── ConversationHeader.tsx    → avatar, name, status, actions
├── theme/
│   ├── ThemeProvider.tsx         → Context + CSS var application
│   ├── FrequencySwitcher.tsx     → 6 curated + custom
│   ├── ThemeStudio.tsx           → builder, preview, export/import
│   └── DensityMotionControls.tsx
├── layout/
│   ├── AppShell.tsx              → three-zone (rail, list, chat, details)
│   ├── AppRail.tsx               → icon navigation
│   ├── DetailsPanel.tsx          → media, members, comfort toggles
│   ├── CommandPalette.tsx        → ⌘K registry
│   ├── SplitView.tsx             → dual chat panels
│   └── PopoutWindow.tsx          → window.open + postMessage sync
└── settings/
    ├── SettingsNav.tsx
    ├── AccountSection.tsx
    ├── PrivacySection.tsx        → granular read receipts per conversation
    ├── NotificationsSection.tsx  → Focus Hours config
    ├── SessionsSection.tsx
    ├── AppearanceSection.tsx     → ThemeStudio entry
    ├── LanguageSection.tsx       → i18n selector
    └── SecuritySection.tsx
```

---

## 4. Error Handling

| Layer | Strategy |
|-------|----------|
| **API (TanStack Query)** | `retry: 3` with exponential backoff; `onError` → toast + Sentry; 401 → auto-refresh token → retry once → logout |
| **WebSocket** | Exponential backoff (1s, 2s, 4s, 8s, max 15s); queue outgoing messages with `clientMessageId`; on reconnect, flush queue + request missed events |
| **Optimistic UI** | On failure: show inline "Retry" on message bubble; preserve draft; toast with undo |
| **Forms (React Hook Form + Zod)** | Inline field errors (never top-level banner); server validation errors mapped to fields; submit button loading state |
| **Boundary** | `ErrorBoundary` per route group; fallback shows "Something went wrong" + "Retry" + "Report" (Sentry) |
| **Offline** | `navigator.onLine` listener → banner "Reconnecting…"; composer stays functional; queue persists in localStorage |

---

## 5. Testing Strategy

| Level | Tool | Coverage Target |
|-------|------|-----------------|
| **Unit** | Vitest + RTL | Stores, hooks, utils, pure components |
| **Component** | Vitest + RTL + MSW | MessageBubble, Composer, ConversationList, ThemeStudio, CommandPalette |
| **Integration** | Vitest + MSW | Auth flow, message send/receive/reconcile, theme switch, WS reconnect |
| **E2E** | Playwright | Critical paths: register→verify→login→chat→send→receive→read receipt→reconnect |
| **Accessibility** | axe-core in CI | All pages/components; WCAG 2.2 AA |
| **Visual Regression** | Storybook + Chromatic | Frequency variants, density/motion, component states |

### 5.1 Test Infrastructure Setup (Phase 1)

```bash
# Vitest
npm i -D vitest @testing-library/react @testing-library/jest-dom @testing-library/user-event jsdom msw

# Playwright
npm i -D @playwright/test
npx playwright install

# Storybook
npx storybook@latest init --type react

# axe-core
npm i -D @axe-core/react axe-core

# Sentry
npm i @sentry/nextjs
```

---

## 6. Backend Coordination (Gaps to Close in Parallel)

| Backend Gap | Frontend Phase Needed | Priority |
|-------------|----------------------|----------|
| Email verification endpoints | Phase 2 (Auth) | P0 |
| Groups CRUD + admin | Phase 3-4 (Groups) | P0 |
| Channels CRUD + subscribe | Phase 3-4 (Channels) | P0 |
| Message delivery/read WS events | Phase 3 (Real-time Core) | P0 |
| Redis presence + `presence.changed` | Phase 3 (Real-time Core) | P0 |
| `conversation.read` WS handler | Phase 3 (Real-time Core) | P0 |
| Forward message API | Phase 4 (Messaging UI) | P1 |
| Session management (list/revoke) | Phase 8 (Settings) | P1 |
| Push subscriptions table + API | Phase 6 (Differentiators) | P2 |
| Rate limiting | All phases | P1 |
| Soft delete "for me/everyone" | Phase 4 (Messaging UI) | P1 |
| Admin dashboard APIs | Phase 8 (Admin) | P2 |

---

## 7. Technical Decisions Summary

| Decision | Implementation |
|----------|----------------|
| **Native WebSocket** | Custom binary/JSON protocol in `lib/ws/client.ts` matching PRD §9 event types; reconnection with exponential backoff + idempotency via `clientMessageId` |
| **shadcn/ui manual** | `npx shadcn@latest add` for each component; map PRD tokens to shadcn CSS vars via `tailwind.config.ts` + `globals.css` |
| **Frequency + shadcn theming** | Frequency CSS vars (`--color-*`, `--motion-*`, `--density-*`) drive shadcn's `--background`, `--foreground`, `--primary`, etc. via `@layer base` overrides |
| **Focused Zustand stores** | `useAuthStore`, `useConversationStore`, `useMessageStore`, `useThemeStore`, `useUIStore` — each with `persist` middleware for drafts/theme |
| **PRD folder structure** | `components/chat/`, `components/theme/`, `components/ui/` (shadcn), `lib/api/`, `lib/ws/`, `lib/stores/`, `hooks/` |
| **Test infra first** | Vitest + RTL config, Playwright config, axe-core, Storybook, MSW handlers — before feature code |
| **i18next** | `i18next`, `react-i18next`, `i18next-http-backend` for dynamic loading; namespaces per feature |

---

## 8. Implementation Phases (Approach A)

### Phase 1: Foundation (Week 1-2)
- shadcn/ui + Tailwind + design tokens (Frequency CSS vars)
- AuthProvider, TanStack Query + Zustand stores, i18next
- Test infrastructure (Vitest, Playwright, axe-core, Storybook, MSW)
- CI pipeline with lint, typecheck, test, a11y

### Phase 2: Auth & Routing (Week 2)
- Real auth forms connected to backend
- Protected routes, session management
- Email verification flow

### Phase 3: Real-Time Core (Week 3-4)
- Native WebSocket client with custom protocol
- Message lifecycle (optimistic → SENT → DELIVERED → READ)
- Reconciliation on reconnect
- Typing indicators, presence

### Phase 4: Messaging UI (Week 4-5)
- Virtualized MessageList, MessageBubble
- Composer with undo-send (4s), drafts, reply/edit/delete
- Pulse Rail, ConversationList

### Phase 5: Theme System (Week 5)
- FrequencyProvider (6 curated + custom)
- Theme Studio (builder, live preview, export/import, profile-photo extraction)
- Density/motion controls

### Phase 6: Differentiators (Week 6-7)
- Command Palette (⌘K registry)
- Local Search Index (IndexedDB + Fuse.js)
- Draft Continuity (localStorage + backend sync)
- Inline Media Quick-Edit (canvas-based)
- Split View & Popout Window
- Focus Hours (notification bundling)
- Granular Read Receipts (per-conversation)

### Phase 7: Accessibility (Week 7)
- Atkinson Hyperlegible font toggle
- Independent text-scale slider (CSS `rem` scaling)
- Color-independent status icons (delivered/read shapes)
- Full keyboard paths for every action

### Phase 8: Settings & Admin (Week 8)
- All 7 settings sections with real API calls
- Admin surface (data-dense, locked Compact density, neutral theme)

### Phase 9: Testing & Deploy (Week 8-9)
- Complete test coverage
- Storybook documentation
- Sentry + Web Vitals
- Vercel deploy config

---

## 9. Definition of Done (per PRD §14)

A frontend feature is done only when:
- [ ] Implemented for every breakpoint (§7 PRD)
- [ ] Respects active Frequency's motion/density settings
- [ ] Optimistic UI reconciles correctly with server state on reconnect
- [ ] Keyboard-operable end to end
- [ ] Passes axe-core accessibility check in CI
- [ ] Loading/empty/error states are designed (not left to defaults)
- [ ] Has component tests and, if core flow, Playwright E2E test
- [ ] Documented in Storybook

---

## 10. Success Criteria (per PRD §15)

Two real users, from the browser alone, can:
1. Create an account, verify email, log in
2. Find each other, start a conversation
3. Send/receive a message with no manual refresh
4. See delivery and read state update live
5. See typing live
6. Send an image
7. Create a group, manage it, search within it
8. Log out, log back in from a different browser
9. Switch Frequencies, build a custom theme in Theme Studio
10. Use the command palette to navigate
11. Recover a queued message after a simulated network drop

All without the interface ever feeling like it's fighting them.

---

**Approval:** ✅ Approved — proceed to implementation planning via `writing-plans` skill.