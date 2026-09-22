# PRD — Frontend Web Application
## Next.js Client for the Real-Time Messaging Platform

**Version:** 1.0
**Statut:** Product Requirements Document
**Companion document:** PRD.md (Backend — Modular Monolith, PostgreSQL, Redis, WebSocket, S3)
**Framework:** Next.js (App Router, TypeScript)
**Deployment target:** Vercel (or any Node/edge-compatible host)

---

## 1. Vision

The backend PRD defines a Telegram-inspired real-time messaging platform. This document defines the **client that sits on top of it** — and its job is not to be "a web version of Telegram." Telegram, WhatsApp, Discord and Slack have already claimed "fast," "simple," and "familiar." Competing on those terms means losing on brand recognition alone.

This frontend competes on a different axis: **the interface adapts to the person using it, instead of asking the person to adapt to it.** Two concrete expressions of that idea anchor the whole product:

1. **A real theming system** — not a light/dark toggle, but a small set of considered visual *and behavioral* modes (motion, density, contrast), plus a theme builder the user can shape and save themselves.
2. **Comfort-first interaction design** — the product removes friction that competitors treat as "normal" (unread-scanning fatigue, no send-undo, no keyboard-only path, no cross-device draft continuity, notification noise).

Everything below — architecture, screens, states, roadmap — serves those two ideas without ever compromising the real-time reliability guarantees already defined in the backend PRD (idempotent sends, optimistic UI that reconciles with server truth, never trusting client state as source of truth).

---

## 2. Design brief grounding (per the product's subject matter)

The product's subject is **conversation** — timing, presence, rhythm between people. The visual identity is built around that idea rather than generic "SaaS chat app" defaults (rounded card grids, one grey shadow on everything, a single neon accent on near-black).

### 2.1 Naming concept: "Wavelength"

Messaging is about tuning into someone else's frequency. This gives the product a coherent internal vocabulary instead of arbitrary feature names:
- Themes are called **Frequencies**.
- The live typing/presence/sending indicator is a small **Pulse** motif (a soft waveform blip, not a generic three-dot bounce).
- The activity-density rail next to the chat list is the **Pulse Rail** (detailed in §6).

### 2.2 Token system (design plan)

**Color — shared structural neutrals (constant across every theme):**

| Token | Hex | Role |
|---|---|---|
| `ink-900` | `#14151C` | Deep charcoal-navy — dark-theme background / light-theme text |
| `ink-700` | `#2B2D3A` | Secondary text, icons |
| `ink-400` | `#6B6E7E` | Tertiary text, timestamps, placeholders |
| `mist-200` | `#C9CBD3` | Borders, dividers |
| `mist-100` | `#E7E8EC` | Surface elevation (light) |
| `paper-050` | `#F5F5F2` | Base background (light) |

**Color — constant brand accent (present in every Frequency, so the brand stays recognizable no matter the theme):**

| Token | Hex | Role |
|---|---|---|
| `signal-500` | `#5B5FEF` | Electric indigo — logo, primary actions, links, unread badge, focus ring |
| `signal-600` | `#484CD1` | Hover/pressed state |

**Semantic:**

| Token | Hex | Role |
|---|---|---|
| `success` | `#2FB67C` | Delivered/online/success |
| `warning` | `#E8A93D` | Muted alerts, rate-limit notices |
| `danger` | `#E5484D` | Errors, destructive actions, reports |

Deliberately **not** used: cream-background-plus-terracotta-serif (the current default "AI-generated" look), acid-green-on-near-black, or the SaaS card kit (identical rounded cards + one grey shadow everywhere). Surfaces are differentiated by *elevation logic tied to real hierarchy* (conversation list vs. message bubbles vs. modals each get a distinct, purposeful treatment), not decoration.

**Type:**
- **UI/body:** Inter — the app's primary working typeface. It's a dense, functional product (hundreds of short strings on screen at once); legibility at 13–15px beats personality here.
- **Marketing/display (landing page only):** Fraunces, a serif with real character, used only for the landing page headline and empty-state illustrationtext — never inside the product chrome itself. This keeps the "personality" moment contained to the one place it earns its keep, instead of fighting for space with dense chat UI.
- **Numeric/timestamps:** tabular figures via `font-variant-numeric: tabular-nums` on Inter — not a separate monospace face bolted on for flavor.

**Layout concept:**
- Product shell: **three-zone layout**, left-aligned content, generous internal padding driven by the density setting (see §6). ASCII concept:

```
┌─────────────────────────────────────────────────────────────┐
│  Rail   │        Conversation List        │   Chat  │ Details│
│ (icons) │  ┌─ Pulse Rail (activity)        │  Panel  │ (opt.) │
│         │  │  search · pinned · list        │         │        │
└─────────────────────────────────────────────────────────────┘
```
- Marketing/landing page: single-column, centered, generous whitespace, one hero statement set in Fraunces, no stacked stat-cards-with-gradient-wash default.

**Motion principle:** motion has a *character*, not a uniform default. Each Frequency declares one of `instant` / `calm` / `minimal` (see §5). Regardless of theme, motion always answers a user action (send, open, expand, confirm) — never decorative on-load animation across multiple elements.

---

## 3. Product goals (frontend scope)

The frontend must deliver a fully working client for every item in the backend's MVP list (register, verify email, profile, user search, private chat, real-time send/receive, sent/delivered/read receipts, typing, presence, image/file send, reply, edit, delete, in-conversation search, groups + administration, notifications, session management, blocking, privacy settings) — **plus** the differentiators in §6, which are treated as first-class MVP scope, not "nice to have later," because they are the product's actual thesis.

### 3.1 Non-goals (v1)
Calls, screen share, stories, bots, mini-apps, payments, AI features, and full E2EE UI are out of scope for v1, matching the backend's V2/V3 split (§13). The component architecture must not block them later (e.g., the composer toolbar and message-type renderer are built as extensible registries from day one).

---

## 4. Personas (frontend lens)

| Persona | What the frontend must optimize for them |
|---|---|
| **Everyday user** | Zero-friction onboarding, instant message feedback, comfortable reading over long sessions |
| **Group admin** | Fast member/permission management without leaving the conversation context |
| **Channel creator** | Clear broadcast-vs-chat visual distinction, composer built for one-to-many posting |
| **Power user** | Full keyboard control, command palette, multi-conversation workflows |
| **Platform admin** | A separate, data-dense admin surface (§12) optimized for scanning, not comfort theming |
| **Accessibility-dependent user** | Screen reader support, motion control, adjustable density/contrast/typeface as first-class settings, not an afterthought |

---

## 5. The theming system ("Frequencies")

This is the feature the product will be known for. It is not a `light`/`dark` boolean — it's a small set of curated, named modes plus a user-buildable custom mode, all implemented on one CSS-variable architecture so switching is instant (no reload, no flash).

### 5.1 Curated Frequencies (ship in MVP)

| Frequency | Concept | Background | Motion | Notes |
|---|---|---|---|---|
| **Daylight** | Default light | `paper-050` | Instant | Everyday default |
| **Midnight** | Default dark | `ink-900` | Calm | Everyday default |
| **Dusk** | Warm, dim, low blue-light | Deep plum-brown, amber-tinted surfaces | Calm | Evening reading comfort |
| **Paper Static** | E-ink inspired | Near-white, true black text | None | Max battery/eye comfort; zero animation; built for accessibility, not aesthetics |
| **Focus Carrier** | Low sensory-load mode | Desaturated neutrals, muted accent | Minimal | Wider spacing, no non-essential motion — built for ADHD/anxiety/sensory-sensitive users |
| **Aurora** | Adaptive | Background gradient subtly shifts with local time of day | Calm | The one "expressive" theme; brand indigo always anchors it so it never drifts off-brand |

Every Frequency keeps `signal-500` as the one constant accent, so the product is recognizable in any mode — the same principle Telegram uses with its blue, applied more deliberately.

### 5.2 Theme Studio (custom Frequencies — key differentiator)

Under **Settings → Appearance**, users can build and save their own Frequency:
- Base luminance: light / dim / dark
- Accent hue: color wheel, or **auto-extract from their profile photo** (Material-You-style, but user-triggered, not silently forced)
- Motion intensity: instant / calm / minimal / none
- Density: compact / cozy / comfortable (line-height and padding scale — see §6.4)
- Live preview pane showing the actual conversation UI, updating in real time as sliders move
- **Save, name, and export** the theme as a small JSON token file
- **Import** a theme file a friend or the community shared

This turns theming into a shareable artifact instead of a locked setting — nobody in this product category currently offers export/import of a full behavioral+visual theme, only color swaps.

### 5.3 Implementation approach
- A single `tokens.css` defines CSS custom properties per Frequency under `[data-frequency="..."]` on `<html>`.
- Tailwind config reads colors via `var(--color-*)` so utility classes stay theme-agnostic in component code.
- Custom (Theme Studio) Frequencies write a generated CSS variable block to `localStorage` for instant reapply and sync to the user's profile via the backend's user settings, so the custom theme follows them across devices/sessions.
- Switching Frequency never triggers a route reload; it's a single attribute swap, so it stays under one animation frame.
- `prefers-reduced-motion` and Paper Static/Focus Carrier's "none"/"minimal" motion settings are enforced at the same central animation utility, so no component can accidentally bypass it.

---

## 6. Comfort & convenience differentiators (beyond theming)

Each of these ships in MVP. Each is framed by the concrete friction it removes.

### 6.1 Pulse Rail
A slim vertical rail beside the conversation list visualizing activity density per conversation over time — a subtle waveform/heat pattern rather than a flat unread-count badge. It answers "where is the activity I care about" at a glance instead of forcing a scroll-and-scan through the whole list. Clicking a point on the rail scrubs to that time period in that conversation. This is the one bold, memorable interaction element in the product — everything else stays disciplined around it.

### 6.2 Command palette (⌘K / Ctrl+K)
Jump to any conversation, run any action (mute, mark read, create group, change Frequency, open settings section) without leaving the keyboard. Built once as a generic registry so new actions plug in without new UI.

### 6.3 Undo Send
A short, configurable grace window (default 4s) after hitting send before the message actually reaches the server — a toast with "Undo" appears. Prevents the "sent to the wrong chat" problem that every competitor treats as a delete-after-the-fact edit.

### 6.4 Adaptive density
Compact / Cozy / Comfortable list and bubble spacing, independent of the Frequency choice, so a user can keep their favorite theme and still choose how much fits on screen.

### 6.5 Draft continuity across devices
Composer drafts (text and attached-but-unsent files' metadata) are persisted per-conversation and synced, so switching from phone to laptop resumes exactly where the user left off — not just locally cached like most clients today.

### 6.6 Local instant search index
Recent conversation history is indexed client-side (a small in-memory/IndexedDB fuzzy index) for sub-50ms search-as-you-type across open conversations, falling back to the backend's Full Text Search (backend §17) for full history. Search should never feel like it's "loading."

### 6.7 Inline media quick-edit
Crop, rotate, or annotate an image directly in the composer before sending — no round trip to an external tool.

### 6.8 Split view & popout
On wide viewports, users can pin a second conversation side-by-side (email-client style), or pop a conversation into a small floating always-on-top window while browsing elsewhere in the app. Both are optional, discoverable, and fully keyboard-dismissible.

### 6.9 Focus Hours (notification intelligence)
Users define quiet windows; the app auto-bundles non-mention notifications from busy groups into a single digest instead of individual pings, and surfaces a clear "3 messages, 1 mention" summary rather than a badge storm. Direct mentions and 1:1s always break through.

### 6.10 Granular read-receipt privacy
Read receipts can be toggled per-conversation, not just globally — mirrors the flexibility group admins already get over permissions on the backend side (backend §20).

### 6.11 Accessibility as a first-class comfort feature, not compliance checkbox
Dyslexia-friendly typeface toggle (swap Inter for Atkinson Hyperlegible), independent text-scale slider (distinct from browser zoom, so layout doesn't break), full keyboard path for every action including message actions, visible focus rings on every interactive element, and status indicators that never rely on color alone (delivered/read use distinct icon shapes, not just tint).

---

## 7. Information architecture & navigation

```
Messenger
├── Chats            (default landing after login)
├── Groups
├── Channels
├── Contacts
├── Saved Messages
└── Settings
    ├── Account
    ├── Privacy
    ├── Notifications
    ├── Sessions
    ├── Appearance      ← Frequencies + Theme Studio
    ├── Language
    └── Security
```

- **Desktop (≥1280px):** persistent icon rail (left) + list panel + chat panel + optional details panel (collapsible), matching the backend PRD's three-zone frontend architecture (backend §7).
- **Tablet (768–1279px):** two-panel — list panel collapses behind a back gesture when a chat is open.
- **Mobile (<768px):** single-panel stack navigation with a bottom tab bar (Chats / Groups & Channels / Contacts / Settings); details panel becomes a full-screen sheet.
- **Ultra-wide (≥1600px):** split view (see §6.8) becomes available.

---

## 8. Screens (maps 1:1 to backend PRD §8, with frontend UX detail)

| Screen | Key frontend behavior |
|---|---|
| **Landing** | Marketing shell, Fraunces headline, single clear CTA pair (Log in / Create account). Server-rendered (RSC) for fast first paint and SEO. |
| **Login** | Email + password, inline validation, "Forgot password" and "Create account" links, loading state on the button itself (no full-page spinner) |
| **Register** | Username availability checked live (debounced), password strength meter, clear inline errors per field (never a single top-level error banner) |
| **Verification** | 6-digit code input with auto-advance per digit, resend with visible cooldown timer |
| **Main Messenger** | Three-zone shell (see §7); conversation list with Pulse Rail; message list virtualized; composer with quick-edit, drafts, undo-send |
| **New Conversation** | Instant client-side filter over cached contacts + debounced server search by username/email/name; empty state invites "Invite a friend" rather than a blank box |
| **Group Creation** | Stepper: name/photo/description → add members (multi-select with search) → review; can be completed via keyboard only |
| **Channel Creation** | Name, public handle with live `@handle` availability check, description, photo, visibility toggle (public/private) with a plain-language explanation next to the toggle, not just a label |
| **User Profile** | Avatar, username, name, bio, status, with clear "this is what others see" preview state when editing your own profile |
| **Settings** | Section list (desktop: side nav; mobile: stacked list → drill-in), Appearance section hosts the full Theme Studio (§5.2) |

---

## 9. Real-time UX & message lifecycle (client side)

Mirrors backend §13/§52/§53 exactly — the frontend must never invent its own truth about message state.

```
User hits Send
   │
   ▼
Optimistic bubble rendered immediately, state = SENDING
   (Undo Send grace window active, §6.3)
   │
   ▼
WS message.send { clientMessageId }
   │
   ├── message.created  → state = SENT       (single tick)
   ├── message.delivered → state = DELIVERED  (double tick)
   └── message.read      → state = READ       (double tick, accent color)
   │
   └── on failure / timeout → state = FAILED, inline "Retry" affordance
```

- Every locally created message carries a `clientMessageId`; on reconnect, the server's reconciliation (backend §53) is trusted over any local guess — the UI never shows a duplicate, it swaps the optimistic bubble for the confirmed one in place.
- **Typing indicator** rendered as the Pulse motif, debounced client-side (start on first keystroke after idle, stop after ~3s of inactivity or on send) — never one event per keystroke.
- **Presence** (online/offline/away) subscribed per open conversation, not globally polled, to keep the WS channel light.

### 9.1 Connection state UX

```
CONNECTED     → no UI chrome (silence is the correct default state)
RECONNECTING  → slim, non-blocking top banner: "Reconnecting…" — composer stays usable, queued locally
DISCONNECTED  → banner escalates after a threshold; outgoing messages remain queued with clientMessageId, auto-flushed on reconnect
```
The UI never blocks input during reconnection — this is the concrete expression of the backend's reliability principle ("a message must not silently disappear on a network problem," backend §5) at the interface layer.

---

## 10. Tech stack

| Layer | Choice | Why |
|---|---|---|
| Framework | **Next.js (App Router)**, TypeScript | File-based routing, RSC for the marketing shell, easy Vercel deployment/maintenance as requested |
| Styling | Tailwind CSS on top of the CSS-variable token system (§5.3) | Utility velocity without losing theme-ability |
| Server cache/data fetching | **TanStack Query** | Caching, retries, pagination (cursor-based, matching backend §54) |
| Client/UI state | **Zustand** | Active conversation, composer drafts, Frequency, connection state |
| Real-time | Native WebSocket client wrapped in a typed event bus (`lib/ws`) | Feeds both Zustand and the TanStack Query cache via optimistic patches |
| List virtualization | `@tanstack/react-virtual` | Long conversations and long member lists stay smooth |
| Motion | CSS transitions + a small motion utility respecting per-Frequency motion character and `prefers-reduced-motion` | Avoids uncontrolled animation sprawl |
| Forms/validation | React Hook Form + Zod | Typed, shared validation schemas with the API layer |
| Testing | Vitest + React Testing Library, Playwright, axe-core in CI | Unit/component, E2E, accessibility |
| Component workshop | Storybook | Design-system documentation and visual regression |
| Error/perf monitoring | Sentry + Web Vitals reporting | Matches backend's observability posture (backend §74) |

### 10.1 Folder structure (App Router)

```
app/
  (marketing)/page.tsx, layout.tsx
  (auth)/login | register | verify | forgot-password
  (app)/
    layout.tsx                 ← three-zone shell
    chats/page.tsx
    chats/[conversationId]/page.tsx
    groups/... channels/... contacts/page.tsx saved/page.tsx
    settings/
      account/ privacy/ notifications/ sessions/ appearance/ language/ security/
components/
  chat/        MessageList, MessageBubble, Composer, TypingPulse, PulseRail
  theme/       ThemeProvider, ThemeStudio, FrequencySwitcher
  ui/          Button, Avatar, Modal, Toast, CommandPalette, Field, ...
lib/
  ws/          client, event bus, reconnection/backoff, idempotency
  api/         typed REST client + TanStack Query hooks
  stores/      zustand stores
  tokens/      Frequency definitions, tokens.css generator
hooks/
styles/tokens.css
```

---

## 11. Performance & accessibility budget

| Metric | Target |
|---|---|
| LCP | < 2.0s |
| INP | < 200ms |
| CLS | < 0.1 |
| Message render after WS event | < 100ms client-side (within backend's <500ms delivery budget, backend §76) |
| Frequency switch | < 1 animation frame, no route reload |
| Accessibility baseline | WCAG 2.2 AA, full keyboard path, visible focus rings, no color-only status signaling |

Performance techniques: route-level code splitting, `next/image` for avatars/media, virtualized lists, prefetch-on-hover for conversation open, skeleton states (not spinners) for anything over ~150ms.

---

## 12. Admin surface

A separate, data-dense route group (e.g., `app/(admin)/`) — deliberately **not** themed with the comfort Frequencies described above. This surface optimizes for scanning density and speed for platform operators, matching the KPIs and sections defined in backend §87 (Users, Groups, Channels, Messages, Reports, Moderation, Files, Sessions, System health, Audit logs). It reuses the same design-system primitives (§10) but with the Compact density setting locked and a neutral, high-contrast-by-default look.

---

## 13. Roadmap (mirrors backend PRD scope split)

**MVP (v1) — frontend build for everything in backend MVP, plus:**
Theme Frequencies (curated set) + Theme Studio, Pulse Rail, Command Palette, Undo Send, adaptive density, draft continuity, local instant search, split view/popout, Focus Hours, granular read-receipt privacy, full accessibility baseline.

**V2 — frontend surfaces for backend V2:**
Voice message recorder/player, reaction picker UI, poll composer/results UI, pinned-message rail, advanced search filters, channel-specific composer/broadcast tools, browser push opt-in flow, scheduled-send UI.

**V3 — frontend surfaces for backend V3:**
Audio/video call UI, screen-share UI, stories UI, bot interaction surfaces, AI assistant surfaces, E2EE status indicators, payments UI, mini-app container UI.

---

## 14. Definition of Done (frontend)

A frontend feature is done only when: it's implemented for every breakpoint (§7); it respects the active Frequency's motion/density settings; optimistic UI reconciles correctly with server state on reconnect; it's keyboard-operable end to end; it passes the axe-core accessibility check in CI; loading/empty/error states are designed (not left to defaults); it has component tests and, if it's a core flow, a Playwright E2E test; and it's documented in Storybook.

---

## 15. Success criteria (frontend, mirrors backend §90)

Two real users, from the browser alone, can: create an account, verify email, log in, find each other, start a conversation, send/receive a message with no manual refresh, see delivery and read state update live, see typing live, send an image, create a group, manage it, search within it, log out, and log back in from a different browser — **and** switch Frequencies, build a custom theme in Theme Studio, use the command palette to navigate, and recover a queued message after a simulated network drop — all without the interface ever feeling like it's fighting them.
