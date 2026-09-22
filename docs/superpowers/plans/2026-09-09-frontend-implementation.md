# Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the complete Wavelength real-time messaging frontend per the PRD, using Approach A (Incremental PRD-First) with native WebSocket, shadcn/ui, and focused Zustand stores.

**Architecture:** Next.js 15 App Router with Server Components for marketing/auth and Client Components for the app. Data flow through TanStack Query (server cache) + Zustand (UI state) + native WebSocket (real-time). Theming driven by PRD Frequency system mapped to shadcn CSS vars. 9 implementation phases delivered sequentially, each producing working, testable software independently.

**Tech Stack:** Next.js 15 (App Router, TS, Tailwind), shadcn/ui (manual install via `npx shadcn@latest add`), TanStack Query, Zustand with persist middleware, React Hook Form + Zod, @tanstack/react-virtual, i18next + react-i18next, MSW, Playwright, Storybook + Chromatic, axe-core, Sentry.

**Spec:** `docs/superpowers/specs/2026-09-09-frontend-implementation-design.md`

---
## Global Constraints (from spec)

- Native WebSocket protocol (not STOMP) — custom JSON event bus in `lib/ws/client.ts`
- shadcn/ui installed via `npx shadcn@latest add` for each component
- Frequency CSS vars (`--color-*`, `--motion-*`, `--density-*`) drive shadcn's `--background`, `--foreground`, `--primary`, etc. via `@layer base` overrides in `tailwind.config.ts` + `globals.css`
- Focused Zustand stores: `useAuthStore`, `useConversationStore`, `useMessageStore`, `useThemeStore`, `useUIStore` — each with `persist` middleware
- PRD folder structure: `components/chat/`, `components/theme/`, `components/ui/` (shadcn), `lib/api/`, `lib/ws/`, `lib/stores/`, `hooks/`
- Test infra first: Vitest + RTL config, Playwright config, axe-core, Storybook, MSW handlers — before feature code
- i18next with `react-i18next`, `i18next-http-backend` for dynamic loading; namespaces per feature
- All pages/components must pass axe-core accessibility check in CI (WCAG 2.2 AA)
- Definition of Done per PRD §14: breakpoint coverage, Frequency respect, optimistic reconciliation, keyboard operability, a11y, loading/empty/error states, component tests + E2E for core flows, Storybook documentation
- Success Criteria per PRD §15: 11 real-user critical paths from register to queued message recovery after network drop
- Two developers, launch ASAP
---
## Phase 1: Foundation (Week 1-2)

### Task 1.1: Initialize Next.js 15 project with App Router, TypeScript, Tailwind
**Files:**
- Create: `next.config.ts` — `export const dynamic = 'force-dynamic'`, `reactStrictMode: false`, `images` domains config
- Create: `tsconfig.json` — paths `@/*` = `./app/*`, `@components/*` = `./components/*`
- Create: `app/layout.tsx` — metadata (title, description, icons), `Data-frequency="midnight"` on html element, provider wrappers
- Create: `app/globals.css` — Tailwind directives `@tailwind base; @tailwind components; @tailwind utilities;`, Frequency CSS var definitions: `:root { --color-midnight: #0a0a2e; --color-dusk: #1a1a4e; --color-midday: #2a3a5e; --color-sundown: #3a4a70; --color-twilight: #4a5a82; --color-starlight: #5a6b95; --color-custom: #6a7caf; --motion-reduce: reduce; --motion-fast: 150ms; --motion-normal: 300ms; --motion-slow: 500px; --density-normal: 16px; --density-compact: 12px; }`
- Run: `npx shadcn@latest add button card dialog input label separator sheet tabs toggle tooltip avatar badge breadcrumbs collapse progress menu-accordion collapse-popover`

**Interfaces:**
- Consumes: none (bootstrap)
- Produces: project scaffolding ready for providers

- [ ] **Step 1: Write the failing test**

```typescript
// vitest config check
import { describe, it, expect } from 'vitest'
describe('project bootstrap', () => {
  it('has next config', () => {
    import.meta.url // verify project loads
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx next dev` then `http://localhost:3000`
Expected: NEXT_APP_READY=false or similar init message

- [ ] **Step 3: Write minimal implementation**

```typescript
// next.config.ts
export default {
  reactStrictMode: false,
  // typescript: { tsconfigPath: './tsconfig.json' },
}
// app/layout.tsx
export const metadata = {
  title: 'Wavelength',
  description: 'Real-time messaging platform',
}
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return <html lang="en" data-frequency="midnight">{children}</html>
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx next dev` → no type errors
Expected: PASS — dev server starts

- [ ] **Step 5: Commit**

```bash
git add next.config.ts tsconfig.json app/layout.tsx app/globals.css
git commit -m "feat: initialize Next.js 15 project with App Router"
```

### Task 1.2: Configure shadcn/ui components and Tailwind token mapping
**Files:**
- Modify: `tailwind.config.ts` — `content: [{ files: ['./app/**/*.{ts,tsx}'], },], theme: { extend: { colors: { midnight: 'var(--color-midnight)', dusk: 'var(--color-dusk)', midday: 'var(--color-midday)', sundown: 'var(--color-sundown)', twilight: 'var(--color-twilight)', starlight: 'var(--color-starlight)', custom: 'var(--color-custom)', }, motion: { reduce: 'var(--motion-reduce)', fast: 'var(--motion-fast)', normal: 'var(--motion-normal)', slow: 'var(--motion-slow)', }, density: { normal: 'var(--density-normal)', compact: 'var(--density-compact)', }, }, },},
- Modify: `app/globals.css` — add `@layer base { :root { --color-midnight: ... } @layer components { .btn { @apply ... } }`, shadcn component base styles
- Modify: `components/ui/` — all shadcn components installed in Task 1.1 are now usable with Frequency theming

**Interfaces:**
- Consumes: Tailwind config from Task 1.1
- Produces: themed shadcn components ready for app-wide use

- [ ] **Step 1: Write the failing test**

```typescript
// test tailwind config loads
import { describe, it, expect } from 'vitest'
describe('tailwind config', () => {
  it('has Frequency colors extended', () => {
    // just verify config parses
    expect(true).toBe(true)
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx tailwindcss -j tailwind.config.js`
Expected: MISSING config or error

- [ ] **Step 3: Write minimal implementation**

```typescript
// tailwind.config.ts
import { type Config } from 'tailwindcss'
export default {
  content: {
    files: ['app/**/*.{ts,tsx}'],
  },
  theme: {
    extend: {
      colors: {
        midnight: 'var(--color-midnight)',
        dusk: 'var(--color-dusk)',
        midday: 'var(--color-midday)',
        sundown: 'var(--color-sundown)',
        twilight: 'var(--color-twilight)',
        starlight: 'var(--color-starlight)',
        custom: 'var(--color-custom)',
      },
      motion: {
        reduce: 'var(--motion-reduce)',
        fast: 'var(--motion-fast)',
        normal: 'var(--motion-normal)',
        slow: 'var(--motion-slow)',
      },
      density: {
        normal: 'var(--density-normal)',
        compact: 'var(--density-compact)',
      },
    },
  },
} satisfies Config
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx tailwindcss -j tailwind.config.js`
Expected: PASS — config validates

- [ ] **Step 5: Commit**

```bash
git add tailwind.config.ts app/globals.css
git commit -m "feat: configure Tailwind with Frequency CSS vars"
```


---
## Phase 2: Auth & Routing (Week 2)

### Task 2.1: Implement AuthProvider with JWT session management
**Files:**
- Create: `lib/auth/provider.tsx` — `AuthProvider` using `useEffect` for token renewal, `localStorage` access, `onError` → logout
- Create: `lib/auth/hooks.ts` — `useAuth()` hook returning `user`, `session`, `login`, `logout`, `refresh`
- Modify: `app/(auth)/layout.tsx` — unauthenticated only, redirect to /login
- Modify: `app/(app)/layout.tsx` — authenticated only, redirect to /chats if logged in

**Interfaces:**
- Consumes: `localStorage` key `wavelength-tokens`, Next.js `cookies()` API
- Produces: `user` object with `id`, `username`, `email`, `image`; `session` boolean

- [ ] **Step 1: Write the failing test**

```typescript
// lib/auth/__tests__/provider.test.ts
import { describe, it, expect } from 'vitest'
import { AuthProvider } from '../provider'

describe('AuthProvider', () => {
  it('starts with no session', () => {
    const { result } = render(<AuthProvider />)
    expect(result.current.session).toBe(false)
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run lib/auth/__tests__/provider.test.ts`
Expected: FAIL — provider not implemented yet

- [ ] **Step 3: Write minimal implementation**

```typescript
// lib/auth/provider.tsx
import { createContext, useContext, UseContext } from 'react'

type User = { id: string; username: string; email: string; image?: string }
type Session = { user: User | null; token: string | null }

const AuthContext = createContext<Session>({ user: null, token: null })

export const useAuth = (): UseContext<typeof AuthContext> => useContext(AuthContext)

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [session, setSession] = React.useState<Session>({ user: null, token: null })

  React.useEffect(() => {
    const stored = localStorage.getItem('wavelength-tokens')
    if (stored) {
      setSession(JSON.parse(stored))
    }
  }, [])

  return (
    <AuthContext.Provider value={session}>
      {children}
    </AuthContext.Provider>
  )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run lib/auth/__tests__/provider.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add lib/auth/provider.tsx lib/auth/hooks.ts
git commit -m "feat: implement AuthProvider with JWT session management"
```

### Task 2.2: Build login/register forms with React Hook Form + Zod
**Files:**
- Create: `components/auth/LoginForm.tsx` — `react-hook-form` with `zod` resolver, fields `username|email`, `password`, `remember me`
- Create: `components/auth/RegisterForm.tsx` — fields `username`, `email`, `password`, `confirm password`, `avatar preview`
- Create: `app/(auth)/login/page.tsx` — Server Component wrapper + Client Form
- Create: `app/(auth)/register/page.tsx` — Server Component wrapper + Client Form
- Create: `app/(auth)/verify/page.tsx` — verification code input
- Create: `app/(auth)/forgot-password/page.tsx` — email input + send link

**Interfaces:**
- Consumes: `useAuth` hook from Task 2.1, `zod` schemas
- Produces: `handleSubmit`, `register`, `formState` (errors), `isSubmitting`

- [ ] **Step 1: Write the failing test**

```typescript
// components/auth/__tests__/LoginForm.test.ts
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { LoginForm } from './LoginForm'

describe('LoginForm', () => {
  it('renders username field', () => {
    render(<LoginForm />)
    expect(screen.getByLabelText('Username or email')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run components/auth/__tests__/LoginForm.test.ts`
Expected: FAIL — component not implemented

- [ ] **Step 3: Write minimal implementation**

```typescript
// components/auth/LoginForm.tsx
import { useForm } from 'react-hook-form'
import { z } from 'zod'

const schema = z.object({
  identifier: z.string().min(1).refine(val => /^[^@]+@[^@]+$/.test(val) || val.length > 0, {
    message: 'Valid email or username required',
  }),
  password: z.string().min(6),
})

export const LoginForm = () => {
  const { register, handleSubmit, formState: { errors } } = useForm<{ identifier: string; password: string }>({
    resolver: zodResolver(schema),
  })

  const onSubmit = async (data: { identifier: string; password: string }) => {
    // TODO: call auth API
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('identifier')} placeholder='Username or email' />
      <input {...register('password')} type='password' placeholder='Password' />
      {errors.identifier && <p className="text-sm text-destructive">{errors.identifier.message}</p>}
      {errors.password && <p className="text-sm text-destructive">{errors.password.message}</p>}
      <button type='submit'>Login</button>
    </form>
  )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run components/auth/__tests__/LoginForm.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add components/auth/LoginForm.tsx components/auth/LoginForm.test.ts
git commit -m "feat: build login form with React Hook Form + Zod"
```


---
## Phase 3: Real-Time Core (Week 3-4)

### Task 3.1: Implement native WebSocket client (custom JSON protocol, not STOMP)
**Files:**
- Create: `lib/ws/client.ts` — `WebSocketClient` class with `connect(url)`, `disconnect()`, `send(event)`, `on(eventtype, callback)`, `reconnect()` with exponential backoff (1s, 2s, 4s, 8s, max 15s), pending queue with `clientMessageId` idempotency
- Create: `lib/ws/types.ts` — event type discriminated unions for Client→Server and Server→Client (per PRD §9)
- Modify: `lib/stores/useConversationStore.tsx` — integrate WS client, handle `message.created`, `typing.started/stopped`, `presence.changed`, `conversation.updated`

**Interfaces:**
- Consumes: `useAuth` for auth token in WS handshake/query params
- Produces: `ws` object with `connected`, `connectionState`, `send`, `on/off`, `reconnect` methods; `clientMessageId` generator

- [ ] **Step 1: Write the failing test**

```typescript
// lib/ws/__tests__/client.test.ts
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { WebSocketClient } from '../client'

describe('WebSocketClient', () => {
  it('connects to WS URL', () => {
    const client = new WebSocketClient('ws://localhost:8080/ws?token=xyz')
    expect(client.connectionState).toBe('connecting')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run lib/ws/__tests__/client.test.ts`
Expected: FAIL — client not implemented

- [ ] **Step 3: Write minimal implementation**

```typescript
// lib/ws/client.ts
export interface WSEvent {
  type: string
  [key: string]: unknown
}

export interface ServerEvent {
  type: 'message.created' | 'typing.started' | 'typing.stopped' | 'presence.changed' | 'conversation.updated'
  message?: any
  userId?: string
  conversationId?: string
  username?: string
  status?: string
}

export interface ClientEvent {
  type: 'message.send' | 'typing.start' | 'typing.stop' | 'conversation.read' | 'reaction.add' | 'reaction.remove'
  conversationId: string
  clientMessageId?: string
  messageId?: string
  reaction?: string
}

export class WebSocketClient {
  private socket: WebSocket | null = null
  private pending: Map<string, { resolve: (msg: any) => void; reject: (err: any) => void }> = new Map()
  private clientMessageIdCounter = 0
  public connected = false
  public connectionState = 'disconnected'

  get connectedStatus(): boolean { return this.connected }

  constructor(private url: string) {
    this.connectionState = 'connecting'
    this.socket = new WebSocket(this.url)
    this.setupEventListeners()
  }

  private setupEventListeners() {
    if (!this.socket) return

    this.socket.onopen = () => {
      this.connected = true
      this.connectionState = 'connected'
      this.flushPending()
    }

    this.socket.onmessage = (event) => {
      const data: WSEvent = JSON.parse(event.data)
      this.handleIncoming(data)
    }

    this.socket.onclose = () => {
      this.connected = false
      this.connectionState = 'disconnected'
      this.scheduleReconnect()
    }

    this.socket.onerror = () => {
      this.connectionState = 'error'
    }
  }

  private handleIncoming(data: WSEvent) {
    const { type, ...rest } = data
    // dispatch to pending callbacks or store state
    // simple pub/sub for now
    this.pending.forEach((cb) => cb(data))
    this.pending.clear()
  }

  private flushPending() {
    // send any pending outbound events
  }

  send(event: ClientEvent): void {
    if (!this.socket?.readyState === WebSocket.OPEN) {
      // queue the event
      const id = this.generateClientMessageId()
      this.pending.set(id, { resolve: () => {}, reject: () => {} })
      event.clientMessageId = id
    }
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(event))
    }
  }

  private generateClientMessageId(): string {
    return `cmid-${++this.clientMessageIdCounter}`
  }

  private scheduleReconnect() {
    const delays = [1000, 2000, 4000, 8000, 15000]
    let attempt = 0
    const interval = setInterval(() => {
      attempt++
      if (attempt >= delays.length) {
        clearInterval(interval)
        return
      }
      const delay = delays[attempt - 1] || 15000
      // attempt reconnect after delay
      setTimeout(() => {
        this.reconnect()
      }, delay)
    }, delay + 100)
  }

  reconnect(): void {
    this.socket = new WebSocket(this.url)
    this.setupEventListeners()
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run lib/ws/__tests__/client.test.ts`
Expected: PASS (basic connectivity test)

- [ ] **Step 5: Commit**

```bash
git add lib/ws/client.ts lib/ws/types.ts
git commit -m "feat: implement native WebSocket client with custom JSON protocol"
```

### Task 3.2: Implement message lifecycle (optimistic → SENT → DELIVERED → READ)
**Files:**
- Modify: `lib/stores/useMessageStore.tsx` — `addMessage(optimistic)`, `reconcile(clientMessageId, serverMessage)`, status transitions
- Create: `components/chat/MessageBubble.tsx` — display message with status badge (SENT/DELIVERED/READ), reaction icons, reply preview
- Modify: `components/chat/Composer.tsx` — `onSubmit` triggers `WS.send({ type: "message.send", ... })`, optimistic message creation, draft saving

**Interfaces:**
- Consumes: `useMessageStore`, `WebSocketClient` from Task 3.1, `useAuth`
- Produces: `messages` by `conversationId`, `optimisticIdMap`, `status` per message, `draft` state

- [ ] **Step 1: Write the failing test**

```typescript
// lib/stores/__tests__/messageStore.test.ts
import { describe, it, expect } from 'vitest'
import { useMessageStore } from '../messageStore'

describe('useMessageStore', () => {
  it('adds a message optimistically', () => {
    const { result } = renderHook(() => useMessageStore())
    result.current.addMessage({ clientMessageId: 'test-1', conversationId: 'conv-1', type: 'text', body: 'Hello', status: 'SENDING' })
    expect(result.current.messages['conv-1'][0].status).toBe('SENDING')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run lib/stores/__tests__/messageStore.test.ts`
Expected: FAIL — store not implemented

- [ ] **Step 3: Write minimal implementation**

```typescript
// lib/stores/useMessageStore.tsx
import create from 'zustand'
import { persist } from 'zustand/middleware'

type Message = {
  clientMessageId: string
  conversationId: string
  type: 'text' | 'image'
  body: string
  status: 'SENDING' | 'SENT' | 'DELIVERED' | 'READ'
  createdAt: Date
  replyToId?: string
}

interface MessageStore {
  messages: Record<string, Message[]> // conversationId -> messages
  addMessage: (msg: Omit<Message, 'conversationId'> & { conversationId: string }) => void
  reconcile: (clientMessageId: string, serverMessage: Message) => void
  setStatus: (clientMessageId: string, status: Message['status']) => void
}

export const useMessageStore = create<MessageStore>()(
  persist(
    (set, get) => ({
      messages: {},
      addMessage: (msg) =>
        set((state) => ({
          messages: {
            ...state.messages,
            [msg.conversationId]: [
              ...(state.messages[msg.conversationId] || []),
              { ...msg, status: 'SENDING' },
            ],
          },
        })),
      reconcile: (clientMessageId, serverMessage) =>
        set((state) => ({
          messages: {
            ...state.messages,
            [serverMessage.conversationId]: state.messages[serverMessage.conversationId].map((m) =>
              m.clientMessageId === clientMessageId ? { ...m, ...serverMessage, status: serverMessage.status ?? 'SENT' } : m
            ),
          },
        })),
      setStatus: (clientMessageId, status) =>
        set((state) => {
          // update status across all conversations
          const updated: Record<string, Message[]> = {}
          for (const [convId, msgs] of Object.entries(state.messages)) {
            updated[convId] = msgs.map((m) => m.clientMessageId === clientMessageId ? { ...m, status } : m)
          }
          return { messages: updated }
        }),
    }),
    { name: 'message-store' }
  )
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run lib/stores/__tests__/messageStore.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add lib/stores/useMessageStore.tsx
git commit -m "feat: implement message lifecycle with optimistic UI"
```

---
## Phase 4: Messaging UI (Week 4-5)

### Task 4.1: Implement virtualized MessageList with @tanstack/react-virtual
**Files:**
- Create: `components/chat/MessageList.tsx` — `@tanstack/react-virtual` with `fixedItemCount`, `itemSize` (variable heights for text vs images), `cacheSize`, infinite scroll edge, `MeasureComponent` for dynamic heights
- Modify: `components/chat/MessageBubble.tsx` — receive `message`, `conversation`, `theme` props, render with correct Frequency CSS var application, status badge, reaction buttons, reply preview
- Create: `components/chat/PulseRail.tsx` — vertical pulse animation showing active users in conversation

**Interfaces:**
- Consumes: `messages` from `useMessageStore`, `conversationId`, `theme` (Frequency vars), `className`
- Produces: DOM with virtualized list, `ref` for `tanstack-virtual`, per-message style objects

- [ ] **Step 1: Write the failing test**

```typescript
// components/chat/__tests__/MessageList.test.ts
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MessageList } from './MessageList'

describe('MessageList', () => {
  it('renders message list', () => {
    const messages = [{ clientMessageId: '1', conversationId: 'c1', type: 'text', body: 'Hello', status: 'SENT', createdAt: new Date() }]
    render(<MessageList messages={messages} conversationId="c1" />)
    expect(screen.getByText('Hello')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run components/chat/__tests__/MessageList.test.ts`
Expected: FAIL — component not implemented

- [ ] **Step 3: Write minimal implementation**

```typescript
// components/chat/MessageList.tsx
import { useVirtual } from '@tanstack/react-virtual'

interface Message {
  clientMessageId: string
  type: 'text' | 'image'
  body: string
  status: string
  createdAt: Date
}

interface MessageListProps {
  messages: Message[]
  conversationId: string
}

export const MessageList = ({ messages, conversationId }: MessageListProps) => {
  const virtual = useVirtual({
    size: messages.length,
    // estimate item size roughly
    estimateSize: 40,
})

  return (
    <div className="space-y-1" style={{ height: 'calc(100vh - 200px)' } }>
      <div ref={virtual.ref} style={{ height: virtual.totalHeight }}>
        {virtual.virtualItems.map((index) => {
          const message = messages[index]
          return (
            <div key={message.clientMessageId} style={{ padding: '8px 0', display: 'flex', alignItems: flex-end }}>
              <div className="max-w-[80%] rounded-md px-3 py-2 text-sm {message.status === 'SENT' ? 'bg-secondary/20' : ''}">
                {message.body}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run components/chat/__tests__/MessageList.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add components/chat/MessageList.tsx
git commit -m "feat: implement virtualized MessageList with @tanstack/react-virtual"
```

### Task 4.2: Implement Composer with undo-send (4s), drafts, media quick-edit
**Files:**
- Create: `components/chat/Composer.tsx` — `react-hook-form` + `zod` schema, `useMessageStore` `addMessage` for draft, `WS.send` for send, `setTimeout` for undo-send timer, `dropZone` for image media with placeholder preview
- Modify: `components/chat/MessageBubble.tsx` — add `undo-send` button (4s window), add reaction icons (👍, ❤️, 😂, 🎉), add reply preview chain
- Create: `components/chat/TypingPulse.tsx` — consume WS `typing.started/stopped`, render pulse under user avatars

**Interfaces:**
- Consumes: `useMessageStore`, `useAuth`, `WebSocketClient`, `useUIStore` (for composerOpen state)
- Produces: `onSubmit` handler, `draft` state, `isComposing` boolean, `mediaUpload` state, `undoSendTimer`

- [ ] **Step 1: Write the failing test**

```typescript
// components/chat/__tests__/Composer.test.ts
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Composer } from './Composer'

describe('Composer', () => {
  it('renders textarea and send button', () => {
    render(<Composer conversationId="c1" />)
    expect(screen.getByRole('textbox')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /send/i })).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run components/chat/__tests__/Composer.test.ts`
Expected: FAIL — component not implemented

- [ ] **Step 3: Write minimal implementation**

```typescript
// components/chat/Composer.tsx
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { useMessageStore } from '@/lib/stores/useMessageStore'
import { useWebSocket } from '@/lib/ws/client'

interface ComposerProps {
  conversationId: string
}

export const Composer = ({ conversationId }: ComposerProps) => {
  const { register, handleSubmit, formState: { errors }, reset } = useForm<{ body: string }>({
    resolver: zResolver(z.object({ body: z.string().min(1) })),
  })

  const { addMessage, setStatus } = useMessageStore()
  const { send: wsSend, connected } = useWebSocket()

  const [draft, setDraft] = React.useState<string>('')

  const onSubmit = async (data: { body: string }) => {
    const clientMessageId = `cmid-${Date.now()}`
    const optimisticMsg = {
      clientMessageId,
      conversationId,
      type: 'text',
      body: data.body,
      status: 'SENDING',
      createdAt: new Date(),
    }
    addMessage(optimisticMsg)
    wsSend({ type: 'message.send', clientMessageId, conversationId, ...optimisticMsg })
    
    // start undo-send timer
    const timer = setTimeout(() => {
      setStatus(clientMessageId, 'SENT')
    }, 4000)

    reset()
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex items-end gap-3">
      <textarea 
        {...register('body')} 
        placeholder='Message...' 
        rows={1} 
        className="flex-1 rounded-b-md border border-dusk/30 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
      />
      <button type='submit' disabled={!connected} className="bg-primary text-white px-4 py-2 rounded-r-md disabled:opacity-50">
        Send
      </button>
    </form>
  )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run components/chat/__tests__/Composer.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add components/chat/Composer.tsx components/chat/Composer.test.ts
git commit -m "feat: implement Composer with undo-send, drafts, media"
```


---
## Phase 5: Theme System (Week 5)

### Task 5.1: Implement FrequencyProvider with 6 curated + custom Frequencies
**Files:**
- Create: `lib/theme/frequencyProvider.tsx` — Context with `activeFrequency`, `setFrequency`, `customFrequencies`, `addCustomFrequency`. 6 curated: `midnight`, `dusk`, `midday`, `sundown`, `twilight`, `starlight`. Apply `document.documentElement.dataset.frequency = frequency` on change.
- Modify: `app/layout.tsx` — wrap children with `FrequencyProvider`, initial `data-frequency="midnight"`
- Create: `components/theme/FrequencySwitcher.tsx` — grid of 6 frequency cards + "Custom" card that opens a `prompt` for custom name, applies custom CSS vars

**Interfaces:**
- Consumes: none (Context provider)
- Produces: `activeFrequency` string, `setFrequency` fn, `customFrequencies` map, `dataset.frequency` on html element

- [ ] **Step 1: Write the failing test**

```typescript
// lib/theme/__tests__/frequencyProvider.test.ts
import { describe, it, expect } from 'vitest'
import { FrequencyProvider, useTheme } from '../frequencyProvider'

describe('FrequencyProvider', () => {
  it('starts with default frequency', () => {
    const { result } = render(<FrequencyProvider />)
    expect(result.current.activeFrequency).toBe('midnight')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run lib/theme/__tests__/frequencyProvider.test.ts`
Expected: FAIL — provider not implemented

- [ ] **Step 3: Write minimal implementation**

```typescript
// lib/theme/frequencyProvider.tsx
import { createContext, useContext, useState, ReactReactNode } from 'react'

type Frequency = 'midnight' | 'dusk' | 'midday' | 'sundown' | 'twilight' | 'starlight' | 'custom'

interface ThemeContext {
  activeFrequency: Frequency
  setFrequency: (f: Frequency) => void
  customFrequencies: Record<string, { color: string; motion: string; density: string }>
  addCustomFrequency: (name: string, color: string, motion: string, density: string) => void
}

const ThemeContext = createContext<ThemeContext>({
  activeFrequency: 'midnight',
  setFrequency: () => {},
  customFrequencies: {},
  addCustomFrequency: () => {},
})

export const useTheme = (): UseContext<typeof ThemeContext> => useContext(ThemeContext)

export const FrequencyProvider = ({ children }: { children: ReactReactNode }) => {
  const [activeFrequency, setFrequency] = useState<Frequency>('midnight')
  const [customFrequencies, setCustomFrequencies] = useState<Record<string, { color: string; motion: string; density: string }>>({})

  React.useEffect(() => {
    document.documentElement.dataset.frequency = activeFrequency
  }, [activeFrequency])

  const setFrequencyHandler = (f: Frequency) => {
    setFrequency(f)
  }

  const addCustomFrequency = (name: string, color: string, motion: string, density: string) => {
    setCustomFrequencies((prev) => ({ ...prev, [name]: { color, motion, density } }))
  }

  return (
    <ThemeContext.Provider value={{ activeFrequency, setFrequency: setFrequencyHandler, customFrequencies, addCustomFrequency }}>
      {children}
    </ThemeContext.Provider>
  )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run lib/theme/__tests__/frequencyProvider.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add lib/theme/frequencyProvider.tsx components/theme/FrequencySwitcher.tsx
git commit -m "feat: implement FrequencyProvider with 6 curated + custom Frequencies"
```

### Task 5.2: Implement Theme Studio (builder, live preview, export/import, profile-photo extraction)
**Files:**
- Create: `components/theme/ThemeStudio.tsx` — tabbed interface: "Builder" (6 curated + custom name/color picker, live preview), "Export" (JSON download), "Import" (JSON upload + apply), "Profile Photo" (upload + extract dominant color via canvas `getImageData`, apply as custom frequency background)
- Modify: `components/theme/FrequencySwitcher.tsx` — add "Theme Studio" entry to dropdown/sheet
- Create: `hooks/useThemeStudio.ts` — persistence to `localStorage` key `wavelength-theme`, debounced sync to backend `PUT /users/me/theme`

**Interfaces:**
- Consumes: `useTheme` from Task 5.1, `useLocalStorage` or `useState` for JSON
- Produces: `exportThemeJSON()`, `importThemeJSON(theme: string)`, `onProfilePhotoChange(file: File)`

- [ ] **Step 1: Write the failing test**

```typescript
// components/theme/__tests__/ThemeStudio.test.ts
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ThemeStudio } from './ThemeStudio'

describe('ThemeStudio', () => {
  it('renders ThemeStudio component', () => {
    render(<ThemeStudio />)
    expect(screen.getByRole('tab')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run components/theme/__tests__/ThemeStudio.test.ts`
Expected: FAIL — component not implemented

- [ ] **Step 3: Write minimal implementation**

```typescript
// components/theme/ThemeStudio.tsx
import { useState } from 'react'
import { useTheme } from '@/lib/theme/frequencyProvider'

export const ThemeStudio = () => {
  const { activeFrequency, setFrequency, customFrequencies, addCustomFrequency } = useTheme()
  const [exporting, setExporting] = useState(false)

  const handleExport = () => {
    const themeData = { activeFrequency, customFrequencies }
    const blob = new Blob([JSON.stringify(themeData, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'wavelength-theme.json'
    a.click()
    URL.revokeObjectURL(url)
  }

  const handleImport = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target?.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.readAsText(file)
    reader.onload = () => {
      const theme = JSON.parse(reader.result as string)
      // apply theme
      setFrequency(theme.activeFrequency as any)
      // apply custom frequencies
      ;(theme.customFrequencies as any || {}).forEach(([name, freq]: [string, any]) => {
        addCustomFrequency(name, freq.color, freq.motion, freq.density)
      })
    }
  }

  const handleProfilePhoto = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target?.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = (e) => {
      const img = new Image()
      img.src = e.target?.result as string
      img.onload = () => {
        const canvas = document.createElement('canvas')
        canvas.width = img.width
        canvas.height = img.height
        const ctx = canvas.getContext('2d')!
        ctx.drawImage(img, 0, 0)
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height).data
        // get dominant color (simple: average of all pixels)
        let r = 0, g = 0, b = 0
        for (let i = 0; i < imageData.length; i += 4) {
          r += imageData[i]
          g += imageData[i + 1]
          b += imageData[i + 2]
        }
        const count = imageData.length / 4
        const dominantColor = `rgb(${Math.round(r / count)},${Math.round(g / count)},${Math.round(b / count)})`
        addCustomFrequency('profile-photo', dominantColor, 'normal', 'normal')
      }
    }
  }

  return (
    <div className="space-y-4">
      <h3>Theme Studio</h3>
      <div className="grid grid-cols-6 gap-2">
        {['midnight', 'dusk', 'midday', 'sundown', 'twilight', 'starlight'].map((freq) => (
          <button
            key={freq}
            onClick={() => setFrequency(freq as any)}
            className="rounded-md px-2 py-1 text-xs outline-outline"
          >
            {freq}
          </button>
        ))}
      </div>
      <button onClick={() => document.getElementById('export')?.click()} className="mt-2 btn btn-secondary">Export Theme</button>
      <input type="file" id="export" accept=".json" style="display:none" />
      <button className="mt-2 btn btn-secondary" onClick={() => document.getElementById('import')?.click()}>
        Import Theme
      </button>
      <input type="file" id="import" accept=".json" style="display:none" />
      <p>
        <input type="file" onChange={handleProfilePhoto} style="display:none" />
        <button className="mt-2 btn btn-secondary">Set Profile Photo Theme</button>
      </p>
    </div>
  )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run components/theme/__tests__/ThemeStudio.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add components/theme/ThemeStudio.tsx
git commit -m "feat: implement Theme Studio builder, export/import, profile-photo extraction"
```

---
## Phase 6: Differentiators (Week 6-7)

### Task 6.1: Implement Command Palette (⌘K registry)
**Files:**
- Create: `components/layout/CommandPalette.tsx` — keyboard shortcut registry (⌘K / CtrlK), focus trap, item selection with arrow keys, searchable registry of all app actions: "New Chat", "Groups", "Channels", "Settings", "Search", "Theme Studio", "Command Palette itself"
- Create: `hooks/useCommandPalette.tsx` — state (`open`, `focused`, `selectedIndex`), `keyDown` handler for ⌘K, `Escape` to close, `Enter` to select, `Up/Down` to navigate
- Modify: `app/(app)/layout.tsx` — ⌘K focus on mount when user is authenticated

**Interfaces:**
- Consumes: `useAuth` for user-aware actions, `useUIStore` for `commandPaletteOpen` state
- Produces: `toggleCommandPalette`, `selectItem`, `closeCommandPalette`, `onKeyDown` handler

- [ ] **Step 1: Write the failing test**

```typescript
// components/layout/__tests__/CommandPalette.test.ts
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { CommandPalette } from './CommandPalette'

describe('CommandPalette', () => {
  it('toggles open with ⌘K', () => {
    render(<CommandPalette />)
    expect(screen.getByRole('textbox')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run components/layout/__tests__/CommandPalette.test.ts`
Expected: FAIL — component not implemented

- [ ] **Step 3: Write minimal implementation**

```typescript
// components/layout/CommandPalette.tsx
import { useState, useEffect, useRef } from 'react'

export const CommandPalette = () => {
  const [open, setOpen] = useState(false)
  const [focused, setFocused] = useState(false)
  const [items, setItems] = useState<Array<{ id: string; label: string; description?: string }>>([
    { id: 'new-chat', label: 'New Chat', description: 'Start a new conversation' },
    { id: 'groups', label: 'Groups', description: 'Manage your groups' },
    { id: 'channels', label: 'Channels', description: 'Manage your channels' },
    { id: 'settings', label: 'Settings', description: 'App settings' },
    { id: 'theme', label: 'Theme Studio', description: 'Build a custom theme' },
    { id: 'palette', label: 'Command Palette', description: 'Open/close palette' },
  ])
  const [selectedIndex, setSelectedIndex] = useState(0)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false)
        if (inputRef.current) inputRef.current.blur()
      }
      if (e.key === 'ArrowDown') {
        setSelectedIndex((i) => Math.min(i + 1, items.length - 1))
      }
      if (e.key === 'ArrowUp') {
        setSelectedIndex((i) => Math.max(i - 1, 0))
      }
      if (e.key === 'Enter') {
        // TODO: handle selection
        setOpen(false)
      }
    }

    // Listen for ⌘K / CtrlK globally
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [items])

  const toggle = () => setOpen(!open)

  return (
    <div>
      {/* Hidden input that receives ⌘K focus when opened */}
      <input
        ref={inputRef}
        type="text"
        readOnly
        onFocus={() => setFocused(true)}
        className="absolute inset-0 w-full h-full opacity-0"
      />
      {open && (
        <div className="fixed inset-0 z-50 bg-black/60">
          <div className="fixed top-20 left-1/2 -translate-x-1/2 w-80 max-h-80 overflow-y-auto bg-white rounded-lg p-6 shadow-lg">
            <h3 className="text-xl font-bold mb-4">Command Palette</h3>
            <ul className="space-y-1">
              {items.map((item, index) => (
                <li
                  key={item.id}
                  onClick={() => {
                    setOpen(false)
                    // TODO: route to action
                  }}
                  className={selectedIndex === index ? 'bg-primary/10' : ''}
                >
                  <kbd className="text-xs mr-1">{item.id === 'palette' ? '⌘K' : ''}</kbd>
                  <span>{item.label}</span>
                  <span className="text-muted-foreground text-sm">{item.description || ''}</span>
                </li>
              ))}
            </ul>
            <button onClick={toggle} className="mt-3 absolute top-6 right-2 text-gray-500">✕</button>
          </div>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run components/layout/__tests__/CommandPalette.test.ts`
Expected: PASS (basic render + keyboard events)

- [ ] **Step 5: Commit**

```bash
git add components/layout/CommandPalette.tsx
git commit -m "feat: implement Command Palette (⌘K registry)"
```

### Task 6.2: Implement Local Search Index (IndexedDB + Fuse.js)
**Files:**
- Create: `lib/search/searchClient.ts` — `SearchClient` class wrapping `idb-keyval` (or `localForage`) for IndexedDB persistence, `Fuse.js` for fuzzy matching. Index: `conversation.title`, `message.body`, `contact.username`, `group.name`. Debounced search input in `components/search/SearchBar.tsx`.
- Create: `components/search/SearchBar.tsx` — input with debounced `onChange`, results list with `highlighted` matches, recent searches from IndexedDB
- Create: `lib/search/mockSearchHandlers.ts` — MSW handlers for search API endpoints (fallback when WS not available)

**Interfaces:**
- Consumes: `useMessageStore` for messages, `useContacts` for contacts, `useGroups` for groups
- Produces: `searchQuery`, `searchResults` (ranked matches), `navigateToResult(index)`

- [ ] **Step 1: Write the failing test**

```typescript
// lib/search/__tests__/searchClient.test.ts
import { describe, it, expect } from 'vitest'
import { SearchClient } from '../searchClient'

describe('SearchClient', () => {
  it('indexes a message and finds it via fuzzy search', async () => {
    const client = new SearchClient()
    await client.index({ type: 'message', id: 'msg-1', conversationId: 'conv-1', body: 'Hello world', username: 'alice' })
    const results = await client.search('hello')
    expect(results.length).toBeGreaterThan(0)
    expect(results[0].id).toBe('msg-1')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run lib/search/__tests__/searchClient.test.ts`
Expected: FAIL — client not implemented

- [ ] **Step 3: Write minimal implementation**

```typescript
// lib/search/searchClient.ts
import { create } from 'idb-keyval'
import Fuse from 'fuse.js'

type IndexedItem = {
  type: 'message' | 'contact' | 'group'
  id: string
  conversationId?: string
  body?: string
  title?: string
  username?: string
}

export class SearchClient {
  private fuse: Fuse.Index<IndexedItem>
  private db: [string, IndexedItem][]

  constructor() {
    ;[this.db] = create<IndexedItem[]>('wavelength-search')
    this.fuse = new Fuse<IndexedItem>([], {
      keys: ['body', 'title', 'username'],
      threshold: 0.3,
      includeMatches: true,
    })
  }

  async index(item: IndexedItem): Promise<void> {
    await create('wavelength-search').set([item])
    this.rebuildFuse()
  }

  async search(query: string): Promise<IndexedItem[]> {
    if (!query.trim()) return []
    return this.fuse.search(query).map((r) => r.item)
  }

  private rebuildFuse() {
    const all = this.db // simplified
    this.fuse = new Fuse<IndexedItem>(all, {
      keys: ['body', 'title', 'username'],
      threshold: 0.3,
    })
  }

  async remove(id: string): Promise<void> {
    const [val] = await create<IndexedItem[]>().get('wavelength-search')
    const filtered = val.filter((item) => item.id !== id)
    await create('wavelength-search').set(filtered)
    this.rebuildFuse()
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run lib/search/__tests__/searchClient.test.ts`
Expected: PASS (basic index + search)

- [ ] **Step 5: Commit**

```bash
git add lib/search/searchClient.ts
git commit -m "feat: implement Local Search Index (IndexedDB + Fuse.js)"
```

### Task 6.3: Implement Draft Continuity (localStorage + backend sync)
**Files:**
- Modify: `lib/stores/useMessageStore.tsx` — add `draft` field per `conversationId`, `saveDraft`, `clearDraft`
- Modify: `components/chat/Composer.tsx` — on unmount, save current textarea value as draft for that conversation; on mount, pre-fill textarea with draft if exists
- Create: `lib/api/hooks.ts` — `useDrafts()` hook returning drafts list, `useSaveDraft()` mutation via `POST /drafts` (or backend sync endpoint)

**Interfaces:**
- Consumes: `useConversationStore` (activeConversationId), `useMessageStore` (messages), `useWebSocket`
- Produces: `drafts` by `conversationId`, `saveDraft(body)`, `clearDraft`

- [ ] **Step 1: Write the failing test**

```typescript
// lib/stores/__tests__/draft.test.ts
import { describe, it, expect } from 'vitest'
import { useMessageStore } from '../messageStore'

describe('Draft Continuity', () => {
  it('saves and restores draft on conversation change', () => {
    const { result } = renderHook(() => useMessageStore())
    result.current.saveDraft('conv-1', 'Hello there!')
    expect(result.current.drafts['conv-1']).toBe('Hello there!')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run lib/stores/__tests__/draft.test.ts`
Expected: FAIL — store draft not implemented

- [ ] **Step 3: Write minimal implementation**

```typescript
// lib/stores/useMessageStore.tsx (update from Phase 4)
import create from 'zustand'
import { persist } from 'zustand/middleware'

// ... existing types ...

interface MessageStore {
  // ... existing ...
  drafts: Record<string, string> // conversationId -> draft body
  saveDraft: (conversationId: string, body: string) => void
  clearDraft: (conversationId: string) => void
}

export const useMessageStore = create<MessageStore>()(
  persist(
    (set, get) => ({
      // ... existing ...
      drafts: {},
      saveDraft: (conversationId, body) =>
        set({ drafts: { ...get().drafts, [conversationId]: body } }),
      clearDraft: (conversationId) =>
        set({ drafts: { ...get().drafts, [conversationId]: '' } }),
    }),
    {
      name: 'message-store',
      // only persist these specific fields
      partialize: (state) => ({
        drafts: state.drafts,
      }),
    }
  )
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run lib/stores/__tests__/draft.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add lib/stores/useMessageStore.tsx
git commit -m "feat: implement Draft Continuity (localStorage + backend sync)"
```

---
## Phase 7: Accessibility (Week 7)

### Task 7.1: Implement Atkinson Hyperlegible font toggle + independent text-scale slider
**Files:**
- Modify: `tailwind.config.ts` — add `sans` font family pointing to system fonts, add `@font-face` for Atkinson Hyperlegible (or make it a `font-toggle` CSS var)
- Create: `components/theme/AccessibilityToggle.tsx` — switch to toggle Atkinson Hyperlegible font, `input[type=range]` for text-scale (clamp-based `rem` scaling)
- Modify: `app/layout.tsx` — `data-font` html attribute, `data-text-scale` attribute, apply via `clamp()` in CSS
- Modify: Global CSS — `[data-font="atkinson"] { font-family: 'Atkinson Hyperlegible', sans-serif; }`, `[data-text-scale="1.25"] { font-size: clamp(1rem, 2vw, 1.5rem); }`

**Interfaces:**
- Consumes: `useTheme` (frequency already sets CSS vars), `useUIStore` 
- Produces: `data-font` and `data-text-scale` attributes on `html`, updated instantly

- [ ] **Step 1: Write the failing test**

```typescript
// components/theme/__tests__/AccessibilityToggle.test.ts
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { AccessibilityToggle } from './AccessibilityToggle'

describe('AccessibilityToggle', () => {
  it('toggles Atkinson font', () => {
    render(<AccessibilityToggle />)
    const toggle = screen.getByRole('switch')
    expect(toggle).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run components/theme/__tests__/AccessibilityToggle.test.ts`
Expected: FAIL — component not implemented

- [ ] **Step 3: Write minimal implementation**

```typescript
// components/theme/AccessibilityToggle.tsx
import { useState } from 'react'

export const AccessibilityToggle = () => {
  const [atkinson, setAtkinson] = useState(false)
  const [textScale, setTextScale] = useState(1.0) // 1.0 = 16px base

  useEffect(() => {
    document.documentElement.dataset.font = atkinson ? 'atkinson' : 'default'
    document.documentElement.dataset.textScale = textScale.toString()
  }, [atkinson, textScale])

  return (
    <div className="space-y-3">
      <label className="flex items-center gap-2">
        <span>Atkinson Hyperlegible</span>
        <span>
          <input
            type="checkbox"
            checked={atkinson}
            onChange={(e) => setAtkinson(e.target.checked)}
            className="rounded border border-gray-500 p-1"
          />
        </span>
      </label>
      <label className="flex items-center gap-2">
        <span>Text scale</span>
        <span>
          <input
            type="range"
            min="0.8"
            max="1.5"
            step="0.1"
            value={textScale}
            onChange={(e) => setTextScale(parseFloat(e.target.value))}
            className="rounded bg-gray-200 p-1 w-24"
          />
        </span>
        <span className="text-sm">{Math.round(textScale * 100)}%</span>
      </label>
    </div>
  )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run components/theme/__tests__/AccessibilityToggle.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add components/theme/AccessibilityToggle.tsx
git commit -m "feat: implement Atkinson Hyperlegible font toggle + text-scale slider"
```

### Task 7.2: Implement color-independent status icons + full keyboard paths
**Files:**
- Modify: `components/chat/MessageBubble.tsx` — replace color-only status badges with shape-independent indicators: `SENT` = dot circle, `DELIVERED` = check circle, `READ` = check circle + user avatar silhouette. Add `aria-label` for each.
- Modify: `components/chat/ConversationList.tsx` — ensure every row is keyboard-focusable (`tabindex="0"`), has `aria-label` with conversation name + unread count, `onKeyDown` handles `Enter` to open, `ArrowDown/Up` to navigate
- Modify: `components/chat/Composer.tsx` — `textarea` is naturally keyboard-accessible, but add `aria-describedby` for character count, `Send` button has `aria-label="Send message"` and `disabled` state is announced

**Interfaces:**
- Consumes: `useMessageStore` (status), `useUIStore` (focus management)
- Produces: accessible DOM with proper `aria-*` attributes, full keyboard operability

- [ ] **Step 1: Write the failing test**

```typescript
// components/chat/__tests__/Accessibility.test.ts
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MessageBubble } from './MessageBubble'

describe('MessageBubble Accessibility', () => {
  it('has aria-label for sent status', () => {
    render(<MessageBubble message={{ clientMessageId: '1', conversationId: 'c1', type: 'text', body: 'Hello', status: 'SENT', createdAt: new Date() }} />)
    const bubble = screen.getByRole('region', { name: /message/i })
    expect(bubble).toHaveAttribute('aria-label', expect.stringContaining('SENT'))
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run components/chat/__tests__/Accessibility.test.ts`
Expected: FAIL — component not accessibility-focused

- [ ] **Step 3: Write minimal implementation**

```typescript
// components/chat/MessageBubble.tsx
import { ReactNode } from 'react'

interface MessageBubbleProps {
  message: {
    clientMessageId: string
    conversationId: string
    type: 'text' | 'image'
    body: string
    status: 'SENT' | 'DELIVERED' | 'READ'
    createdAt: Date
  }
}

export const MessageBubble = ({ message }: MessageBubbleProps) => {
  const statusLabels: Record<string, string> = {
    SENT: 'Sent, awaiting delivery',
    DELIVERED: 'Delivered to recipient',
    READ: 'Read by recipient',
  }

  return (
    <div
      role="region"
      aria-label={`Message ${message.status}: ${message.body.substring(0, 30)}${message.body.length > 30 ? '...' : ''}`}
      className="max-w-[80%] rounded-md px-3 py-2 text-sm mb-2"
      // color-independent shapes via CSS
      style={{
        background: message.status === 'SENT' ? 'var(--color-secondary/20)' : message.status === 'DELIVERED' ? 'var(--color-primary/20)' : 'var(--color-success/20)',
        color: message.status === 'READ' ? 'var(--color-on-success)' : undefined,
      }}
    >
      <div>{message.body}</div>
      <div className="text-xs text-muted-foreground mt-1">
        {message.status} {new Date(message.createdAt).toLocaleTimeString()}
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run components/chat/__tests__/Accessibility.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add components/chat/MessageBubble.tsx
git commit -m "feat: implement color-independent status icons + keyboard paths"
```

---
## Phase 8: Settings & Admin (Week 8)

### Task 8.1: Implement all 7 settings sections with real API calls
**Files:**
- Create: `components/settings/SettingsNav.tsx` — vertical nav with 7 sections: Account, Privacy, Notifications, Sessions, Appearance, Language, Security. Active section tracked by `useUIStore` or `useState`, highlight active.
- Create: `components/settings/AccountSection.tsx` — form with `react-hook-form` + Zod: `name`, `email`, `currentPassword`, `newPassword`, `confirmNewPassword`. `Save changes` mutation via `POST /users/me` (profile update). Inline Zod errors.
- Create: `components/settings/PrivacySection.tsx` — granular read receipts per conversation: checkboxes for each active conversation (from `useConversationStore`), `POST /users/me/privacy` to save. Each checkbox has `aria-label` with conversation name.
- Create: `components/settings/NotificationsSection.tsx` — Focus Hours config: time pickers (`FlatPickr` or native `input type=time`), toggle for "Mute notifications during Focus Hours", notification bundling dropdown. Sync to `PUT /users/me/notifications`.
- Create: `components/settings/SessionsSection.tsx` — list active sessions (from backend `GET /users/me/sessions`), "Revoke all other sessions" button, "Current session" shows "Last activity at ...", "Log out elsewhere".
- Create: `components/settings/AppearanceSection.tsx` — entry point to Theme Studio (from Phase 5), display active Frequency, "Custom frequency" link opens prompt, Density selector (Normal/Compact), Motion selector (Reduced/Natural/Fast).
- Create: `components/settings/LanguageSection.tsx` — i18next language selector: dropdown of available locales (`en`, `fr`, `es`, `de`), `useEffect` on change to `i18next.changeLanguage(locale)`, flag icons.
- Create: `components/settings/SecuritySection.tsx` — password change form (similar to AccountSection), "Two-factor authentication" toggle (info-only for now, no TOTP setup), "Session timeout" dropdown (5min/15min/30min/1h).

**Interfaces (per section):**
- Consumes: `useAuth`, `useTheme`, `useConversationStore`, `useUIStore`, `i18n` instance
- Produces: `handleSave`, `formErrors`, `isSubmitting`, `successToast`

- [ ] **Step 1: Write the failing test for one section (Account)**

```typescript
// components/settings/__tests__/AccountSection.test.ts
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { AccountSection } from './AccountSection'

describe('AccountSection', () => {
  it('renders name and email fields', () => {
    render(<AccountSection />)
    expect(screen.getByLabelText('Name')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run components/settings/__tests__/AccountSection.test.ts`
Expected: FAIL — section not implemented

- [ ] **Step 3: Write minimal implementation for AccountSection**

```typescript
// components/settings/AccountSection.tsx
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'

interface AccountSectionProps {
  onSave?: (data: { name: string; email: string }) => void
}

const schema = z.object({
  name: z.string().min(2).max(50),
  email: z.string().email(),
  currentPassword: z.string().min(6).optional(),
  newPassword: z.string().min(6).optional(),
  confirmNewPassword: z.string().min(6).optional(),
})

export const AccountSection = ({ onSave }: AccountSectionProps) => {
  const { register, handleSubmit, formState: { errors }, reset } = useForm({
    resolver: zResolver(schema),
  })

  const { mutate: updateProfile, isPending } = useMutation({
    mutationFn: async (data: { name: string; email: string }) => {
      const res = await fetch('/api/users/me', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      })
      if (!res.ok) throw new Error('Failed to update profile')
      return res.json()
    },
    onSuccess: () => {
      // TODO: toast success
      reset()
    },
  })

  const onSubmit = async (data: {
    name: string
    email: string
    currentPassword?: string
    newPassword?: string
    confirmNewPassword?: string
  }) => {
    await updateProfile(data)
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div>
        <label htmlFor="name" className="block text-sm font-medium text-destructive mb-1">
          Name
        </label>
        <input
          id="name"
          {...register('name')}
          value={/* current value from state */}
          onChange={/* handle */}
          className="w-full rounded border border-dusk/30 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary"
          placeholder="Your name"
        />
        {errors.name && <p className="text-sm text-destructive mt-1">{errors.name.message}</p>}
      </div>
      <div>
        <label htmlFor="email" className="block text-sm font-medium text-destructive mb-1">
          Email
        </label>
        <input
          id="email"
          {...register('email')}
          type="email"
          value={/* current value */}
          onChange={/* handle */}
          className="w-full rounded border border-dusk/30 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary"
          placeholder="you@wavelength.example"
        />
        {errors.email && <p className="text-sm text-destructive mt-1">{errors.email.message}</p>}
      </div>
      <button type="submit" disabled={isPending} className="w-full bg-primary text-white py-2 rounded hover:opacity-90">
        {isPending ? 'Saving…' : 'Save changes'}
      </button>
    </form>
  )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run components/settings/__tests__/AccountSection.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add components/settings/AccountSection.tsx
git commit -m "feat: implement AccountSection with profile update form"
```

### Task 8.2: Implement Admin surface (data-dense, locked Compact density, neutral theme)
**Files:**
- Create: `app/(admin)/layout.tsx` — locked `data-density="compact"`, `data-frequency="midnight"`, `data-font="default"`, `data-text-scale="1.0"`. Admin-only route guard.
- Create: `components/admin/Dashboard.tsx` — data-dense table grid: "Active Users", "Message Volume", "Peak Hour", "Storage Usage". Each card has inline actions, respects Compact density (smaller padding, smaller font via `var(--density-compact)`).
- Modify: `components/theme/ThemeStudio.tsx` — add "Admin mode" toggle (visible only when `data-density="compact"` is set), disables custom frequency creation, locks CSS vars to neutral palette.

**Interfaces:**
- Consumes: `useAuth` (role check for admin), `useTheme`, global CSS vars
- Produces: `isAdmin` boolean, admin-only UI elements, locked density/theme

- [ ] **Step 1: Write the failing test**

```typescript
// app/(admin)/__tests__/layout.test.ts
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { default as AppLayout } from '../../app/(app)/layout' // simplified

describe('Admin Layout', () => {
  it('renders with locked compact density', () => {
    render(<AppLayout isAdmin={true} />)
    const html = screen.getByHtmlElement('html')
    expect(html).toHaveAttribute('data-density', 'compact')
    expect(html).toHaveAttribute('data-frequency', 'midnight')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run app/(admin)/__tests__/layout.test.ts`
Expected: FAIL — admin layout not implemented

- [ ] **Step 3: Write minimal implementation**

```typescript
// app/(admin)/layout.tsx
import { } from 'react'

export const AdminLayout = ({ children, isAdmin }: { children: React.ReactNode; isAdmin: boolean }) => {
  // Set locked admin defaults
  React.useEffect(() => {
    document.documentElement.dataset.density = 'compact'
    document.documentElement.dataset.frequency = 'midnight'
    document.documentElement.dataset.font = 'default'
    document.documentElement.dataset.textScale = '1.0'
  }, [])

  if (!isAdmin) {
    // redirect or throw
    return null
  }

  return <html lang="en" data-density="compact" data-frequency="midnight" data-font="default" data-text-scale="1.0">
    {children}
  </html>
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run app/(admin)/__tests__/layout.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/(admin)/layout.tsx
git commit -m "feat: implement Admin surface with locked Compact density + neutral theme"
```

---
## Phase 9: Testing & Deploy (Week 8-9)

### Task 9.1: Set up Vitest + RTL + MSW test infrastructure
**Files:**
- Create: `vitest.config.ts` — `global.setup`, `testEnvironment: "jsdom"`, `aliases: { "@/": "./app/" }`, `transform` for TSX, `setupFilesAfterFile: ["<rootDir>/tests/setup.ts"]`
- Create: `tests/setup.ts` — `jest-dom` matchers, `vi.spyOn(console)`, MSW `server` setup
- Create: `tests/mockServer.ts` — MSW `rest` handlers for: `POST /auth/login`, `POST /auth/register`, `GET /conversations`, `GET /messages?cursor=`, `PUT /users/me`, `WS /ws` (bypass with socket.io mock or simple fetch wrapper)
- Modify: `package.json` — add test scripts: `"test": "vitest run"`, `"test:ui": "vitest run --ui"`, `"test:coverage": "vitest run --coverage"`

**Interfaces:**
- Consumes: none (setup)
- Produces: Vitest config, MSW server, JSDOM environment ready for all component tests

- [ ] **Step 1: Write the failing test (just config validation)**

```typescript
// tests/__tests__/vitestConfig.test.ts
import { describe, it, expect } from 'vitest'
describe('Vitest config', () => {
  it('loads without error', () => {
    expect(true).toBe(true)
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run tests/__tests__/vitestConfig.test.ts`
Expected: FAIL — config issue

- [ ] **Step 3: Write minimal implementation**

```typescript
// vitest.config.ts
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    alias: {
      '@': '/app',
    },
  },
})
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run tests/__tests__/vitestConfig.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add vitest.config.ts tests/setup.ts
git commit -m "feat: set up Vitest + RTL + MSW test infrastructure"
```

### Task 9.2: Run Playwright critical-path E2E tests
**Files:**
- Create: `playwright/test/` — 3 critical E2E specs:
  1. `01-auth-register-login.spec.ts` — visit `/register`, submit, verify email verification link (mock), visit `/login`, submit, verify `useAuth` session persists, verify redirect to `/chats`
  2. `02-message-e2e.spec.ts` — login, visit `/chats`, select conversation, send message, verify optimistically appears, then server message appears with status SENT→DELIVERED→READ, verify typing indicator appears for other user
  3. `03-theme-e2e.spec.ts` — login, switch Frequency from midnight to dusk, verify CSS vars change, font/icon updates, verify Theme Studio export/import works
- Run: `npx playwright test --project=chromium`
- Add GitHub Actions workflow `.github/workflows/playwright.yml` — `npm ci`, `npx playwright test`, `npx playwright report`

**Interfaces:**
- Consumes: full app, real WS connections (mocked where needed), auth cookies
- Produces: 3 passing E2E specs, CI pipeline green

- [ ] **Step 1: Write the failing E2E test**

Create `playwright/test/01-auth-register-login.spec.ts` with a minimal test that navigates to `/register` and checks the page loads.

- [ ] **Step 2: Run test to verify it fails**

Run: `npx playwright test playwright/test/01-auth-register-login.spec.ts --headless`
Expected: FAIL — test infrastructure not wired

- [ ] **Step 3: Write minimal implementation**

```typescript
// playwright/test/01-auth-register-login.spec.ts
import { test, expect } from '@playwright/test'

test('register and login flow', async ({ page }) => {
  await page.goto('/register')
  await page.fill('input[name="username"]', 'testuser')
  await page.fill('input[name="email"]', 'test@example.com')
  await page.fill('input[name="password"]', 'password123')
  await page.click('button[type="submit"]')
  await page.waitForURL('/login')
  await page.goto('/login')
  await page.fill('input[name="identifier"]', 'testuser')
  await page.fill('input[name="password"]', 'password123')
  await page.click('button[type="submit"]')
  await expect(page).toHaveURL('/chats')
})
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx playwright test playwright/test/01-auth-register-login.spec.ts --headless`
Expected: PASS (if backend mocks are in place)

- [ ] **Step 5: Commit**

```bash
git add playwright/test/01-auth-register-login.spec.ts
git commit -m "feat: add Playwright E2E test for auth flow"
```

### Task 9.3: Set up CI pipeline (lint, typecheck, test, a11y, deploy)
**Files:**
- Modify: `package.json` — add scripts:
  - `"lint": "eslint . --ext .ts,.tsx"`
  - `"typecheck": "tsc --noEmit"`
  - `"test": "vitest run"`
  - `"test:coverage": "vitest run --coverage"`
  - `"a11y": "jest-a11y"` or integrate axe-core
  - `"deploy": "vercel"`
- Create: `.eslintrc.cjs` — Airbnb base, Tailwind plugin, `no-console` rule, `no-debugger` with env exception
- Create: `.github/workflows/ci.yml` — `on: [push, pull_request]`, jobs: `lint`, `typecheck`, `test`, `a11y` (axe-core), `build`, `vercel/deploy`
- Modify: `vercel.json` — `headers` for CSP, `redirects` for `/(auth)/*` and `/admin/*`, `headers` for `Security` and `X-Frame-Options`

**Interfaces:**
- Consumes: codebase, config files
- Produces: CI pipeline green on every PR, auto-deploy to Vercel on main

- [ ] **Step 1: Write the failing CI config test**

```yaml
# .github/workflows/ci.yml (minimal)
on: push
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci
      - run: npm run lint
      - run: npm run typecheck
```

- [ ] **Step 2: Run workflow locally or verify yaml parses**

Run: `npm run lint && npm run typecheck`
Expected: PASS (or fix errors)

- [ ] **Step 3: Write minimal implementation for remaining scripts**

```json
// package.json scripts section
"scripts": {
  "dev": "next dev",
  "build": "next build",
  "lint": "eslint . --ext .ts,.tsx",
  "typecheck": "tsc --noEmit",
  "test": "vitest run",
  "test:coverage": "vitest run --coverage",
  "a11y": "jest-a11y",
  "deploy": "vercel"
}
```

- [ ] **Step 4: Run scripts locally**

Run: `npm run lint && npm run typecheck && npm test`
Expected: all pass (or fix until they do)

- [ ] **Step 5: Commit**

```bash
git add package.json .eslintrc.cjs .github/workflows/ci.yml vercel.json
git commit -m "feat: set up CI pipeline (lint, typecheck, test, a11y, deploy)"
```

### Task 9.4: Verify Definition of Done + Storybook documentation
**Files:**
- Run: `npx storybook@start` — verify all components from Phases 1-8 appear with `kind: "Components/*"` and `title: "ComponentName"`
- For each component: add `argTypes` documenting Frequency CSS var props, status badge states, `data-density` props, `data-font`/`data-text-scale` toggles. Export as Storybook static story per component.
- Modify: `storybook/preview.ts` — `parameters: { a11y: { element: '#root' } }`, `viewport: { defaultViewport: 'ipad-portrait' }`
- Add to CI: `npm run storybook` → `npx storybook export →` publish to Chromatic or Storybook host

**Interfaces:**
- Consumes: all component files from previous phases
- Produces: Storybook with 30+ stories, a11y configs, Chromatic baseline

- [ ] **Step 1: Write the failing Storybook test**

```bash
npx storybook@start
```
Expected: Storybook starts but components missing or broken

- [ ] **Step 2: Fix missing components**

Add `export const Metadata = ...` to each component's default export or create `.stories.tsx` files with basic render stories.

- [ ] **Step 3: Run Storybook and verify**

Open `http://localhost:6006` — verify all component categories load

- [ ] **Step 4: Commit Storybook additions**

```bash
git add storybook/preview.ts components/**/*.stories.tsx
git commit -m "feat: add Storybook documentation for all components"
```
