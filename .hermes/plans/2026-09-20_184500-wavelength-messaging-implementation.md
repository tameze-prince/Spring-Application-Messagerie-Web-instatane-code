# Wavelength Real-Time Messaging Platform — Complete Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task with two-stage review (spec compliance + code quality).

**Goal:** Complete the Wavelength real-time messaging platform: Spring Boot modular monolith backend + Next.js 15 frontend with Frequency theming system, real-time WebSocket, and all MVP features per PRD.

**Architecture:** 
- Backend: Spring Boot 3.4.3 (Java 21), Spring Modulith, PostgreSQL 16 + Flyway, Redis 7, MinIO (S3), JWT + WebSocket (STOMP)
- Frontend: Next.js 15 App Router, TypeScript, Tailwind CSS, TanStack Query, Zustand, native WebSocket (custom JSON protocol), shadcn/ui
- Theming: 6 curated Frequencies + Theme Studio (user-buildable), CSS variables drive shadcn tokens

**Tech Stack:** 
- Backend: Maven, JUnit 5, Testcontainers, Spring Security, Spring Data JPA, Flyway, Spring WebSocket, AWS SDK S3
- Frontend: Vitest, Playwright, React Hook Form + Zod, @tanstack/react-virtual, i18next, MSW, Storybook, axe-core
- Infra: Docker Compose (PostgreSQL, Redis, MinIO), GitHub Actions CI

---

## Phase 0: Environment Verification (COMPLETED ✓)

- [x] Java 21, Docker, Node.js 22, kubectl, Helm, Terraform, Ansible installed
- [x] Backend tests pass: 4/4 (H2 in-memory for tests)
- [x] Frontend dependencies installed (npm install --legacy-peer-deps)
- [x] Frontend test runner works (no test files yet — expected)

---

## Phase 1: Backend Infrastructure & Core Modules

### Task 1.1: Verify Docker Compose Infrastructure Starts

**Objective:** Confirm PostgreSQL, Redis, MinIO start correctly via docker-compose.dev.yml

**Files:**
- Read: `backend/docker-compose.dev.yml`
- Test: `backend/docker-compose.dev.yml`

**Step 1: Start infrastructure**
```bash
cd backend && docker compose -f docker-compose.dev.yml up -d
```

**Step 2: Verify services**
```bash
# PostgreSQL
docker exec -it $(docker ps -qf "name=postgres") psql -U postgres -d tuto_db -c "SELECT 1;"
# Redis
docker exec -it $(docker ps -qf "name=redis") redis-cli ping
# MinIO
curl -f http://localhost:9000/minio/health/live
```

**Expected:** All three services respond healthy

**Step 3: Commit**
```bash
git add -A && git commit -m "chore: verify docker compose infrastructure"
```

---

### Task 1.2: Run Backend with Dev Profile Against Real Infrastructure

**Objective:** Start Spring Boot with `dev` profile connected to real PostgreSQL/Redis/MinIO

**Files:**
- Modify: `backend/src/main/resources/application-dev.yml` (if needed)
- Test: `backend/docker-compose.dev.yml`

**Step 1: Start infrastructure**
```bash
cd backend && docker compose -f docker-compose.dev.yml up -d
```

**Step 2: Run backend**
```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Step 3: Verify endpoints**
```bash
# Swagger UI
curl -f http://localhost:8080/swagger-ui.html
# Health
curl -f http://localhost:8080/actuator/health
# WebSocket endpoint
# (test via frontend later)
```

**Expected:** App starts, Swagger accessible, health UP

**Step 4: Stop and commit**
```bash
# Ctrl+C to stop backend
docker compose -f docker-compose.dev.yml down
git add -A && git commit -m "chore: verify backend runs with real infrastructure"
```

---

### Task 1.3: Verify Flyway Migrations Apply Cleanly

**Objective:** Ensure all database migrations run without errors on clean PostgreSQL

**Files:**
- Read: `backend/src/main/resources/db/migration/`
- Test: `backend/docker-compose.dev.yml`

**Step 1: Clean database**
```bash
cd backend && docker compose -f docker-compose.dev.yml down -v && docker compose -f docker-compose.dev.yml up -d
```

**Step 2: Run backend to trigger migrations**
```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# Wait for "Started TutoApplication in X seconds"
# Then Ctrl+C
```

**Step 3: Verify tables**
```bash
docker exec -it $(docker ps -qf "name=postgres") psql -U postgres -d tuto_db -c "\dt"
```

**Expected:** All tables from PRD §23 present (users, user_sessions, conversations, conversation_members, messages, message_attachments, message_reactions, message_reads, groups, channels, notifications, blocks, reports, files)

**Step 4: Commit**
```bash
git add -A && git commit -m "chore: verify flyway migrations apply cleanly"
```

---

### Task 1.4: Run Full Backend Test Suite with Testcontainers

**Objective:** Execute all tests including Testcontainers integration tests

**Files:**
- Test: `backend/src/test/`

**Step 1: Run tests**
```bash
cd backend && ./mvnw test
```

**Step 2: Verify results**
Expected: BUILD SUCCESS, all tests pass (currently 4 unit tests; integration tests will be added)

**Step 3: Commit**
```bash
git add -A && git commit -m "test: backend test suite passes"
```

---

## Phase 2: Backend Auth Module (MVP)

### Task 2.1: Register Endpoint — POST /api/v1/auth/register

**Objective:** Implement user registration with validation, password hashing, email verification token

**Files:**
- Read: `backend/src/main/java/spring4/tuto/auth/controller/AuthController.java`
- Read: `backend/src/main/java/spring4/tuto/auth/service/AuthService.java`
- Read: `backend/src/main/java/spring4/tuto/auth/dto/RegisterRequest.java`
- Read: `backend/src/main/java/spring4/tuto/user/domain/User.java`
- Read: `backend/src/main/java/spring4/tuto/user/repository/UserRepository.java`
- Create: `backend/src/test/java/spring4/tuto/auth/AuthControllerRegisterTest.java`

**Step 1: Write failing test**
```java
// backend/src/test/java/spring4/tuto/auth/AuthControllerRegisterTest.java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerRegisterTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    
    @Test
    void register_createsUser_returnsTokens() throws Exception {
        var request = """
            {"username":"testuser","email":"test@example.com","password":"password123","confirmPassword":"password123"}
            """;
        
        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.user.username").value("testuser"));
        
        assertThat(userRepository.findByUsername("testuser")).isPresent();
    }
}
```

**Step 2: Run test to verify failure**
```bash
cd backend && ./mvnw test -Dtest=AuthControllerRegisterTest
# Expected: FAIL - endpoint not implemented or validation missing
```

**Step 3: Implement minimal register endpoint**
- AuthController.register(): validate request, call AuthService.register()
- AuthService.register(): check username/email unique, hash password (BCrypt), save User, generate JWT + refresh token, save UserSession, return AuthResponse
- User entity: add emailVerified=false default, passwordHash field

**Step 4: Run test to verify pass**
```bash
cd backend && ./mvnw test -Dtest=AuthControllerRegisterTest
# Expected: PASS
```

**Step 5: Run full suite**
```bash
cd backend && ./mvnw test
```

**Step 6: Commit**
```bash
git add backend/src/main/java/spring4/tuto/auth/ backend/src/main/java/spring4/tuto/user/ backend/src/test/java/spring4/tuto/auth/AuthControllerRegisterTest.java
git commit -m "feat(auth): implement register endpoint with JWT tokens"
```

---

### Task 2.2: Login Endpoint — POST /api/v1/auth/login

**Objective:** Implement login with email/username + password, return access + refresh tokens

**Files:**
- Modify: `backend/src/main/java/spring4/tuto/auth/controller/AuthController.java`
- Modify: `backend/src/main/java/spring4/tuto/auth/service/AuthService.java`
- Read: `backend/src/main/java/spring4/tuto/auth/dto/LoginRequest.java`
- Read: `backend/src/main/java/spring4/tuto/auth/dto/AuthResponse.java`
- Create: `backend/src/test/java/spring4/tuto/auth/AuthControllerLoginTest.java`

**Step 1: Write failing test**
```java
// AuthControllerLoginTest.java
@Test
void login_withValidCredentials_returnsTokens() throws Exception {
    // Given: user exists (create via repository)
    var user = new User();
    user.setUsername("testuser");
    user.setEmail("test@example.com");
    user.setPasswordHash(passwordEncoder.encode("password123"));
    user.setEmailVerified(true);
    userRepository.save(user);
    
    var request = """{"identifier":"testuser","password":"password123"}""";
    
    mvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists())
        .andExpect(jsonPath("$.refreshToken").exists());
}
```

**Step 2: Run test to verify failure**

**Step 3: Implement login**
- AuthService.login(): find by username OR email, verify password, check emailVerified, generate tokens, save session

**Step 4: Run test to verify pass**

**Step 5: Commit**

---

### Task 2.3: Refresh Token Endpoint — POST /api/v1/auth/refresh

**Objective:** Implement access token refresh using valid refresh token

**Files:**
- Modify: `AuthController.java`, `AuthService.java`
- Read: `RefreshTokenRequest.java`, `AuthResponse.java`
- Create: `AuthControllerRefreshTest.java`

**Step 1: Write failing test**
```java
@Test
void refresh_withValidRefreshToken_returnsNewAccessToken() throws Exception {
    // Given: login first to get refresh token
    var loginResult = loginAndGetTokens("testuser", "password123");
    
    var request = String.format("{\"refreshToken\":\"%s\"}", loginResult.refreshToken());
    
    mvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists())
        .andExpect(jsonPath("$.refreshToken").value(loginResult.refreshToken())); // same refresh token
}
```

**Step 2-6: TDD cycle**

---

### Task 2.4: Logout Endpoint — POST /api/v1/auth/logout

**Objective:** Revoke refresh token (soft delete session)

**Files:**
- Modify: `AuthController.java`, `AuthService.java`
- Create: `AuthControllerLogoutTest.java`

**Step 1: Write failing test** - revoked session cannot refresh

**Step 2-6: TDD cycle**

---

### Task 2.5: Email Verification Flow

**Objective:** Verify email via 6-digit code sent on register, resend capability

**Files:**
- Create: `VerificationController.java`, `VerificationService.java`
- Modify: `AuthService.register()` to send verification email (mock for now)
- Create: `VerificationControllerTest.java`

**Endpoints:**
- `POST /api/v1/auth/verify` - submit code
- `POST /api/v1/auth/verify/resend` - resend code

**Step 1: Write failing test** - register → verify code → login works

**Step 2-6: TDD cycle**

---

### Task 2.6: Forgot/Reset Password

**Objective:** Request reset link, validate token, set new password

**Files:**
- Create: `PasswordResetController.java`, `PasswordResetService.java`
- Create: `PasswordResetControllerTest.java`

**Endpoints:**
- `POST /api/v1/auth/forgot-password` - email → send reset link
- `POST /api/v1/auth/reset-password` - token + new password

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 2.7: JWT Authentication Filter & Security Config

**Objective:** Protect all endpoints except auth endpoints with JWT validation

**Files:**
- Read: `backend/src/main/java/spring4/tuto/auth/security/JwtAuthenticationFilter.java`
- Read: `backend/src/main/java/spring4/tuto/auth/security/SecurityConfig.java`
- Read: `backend/src/main/java/spring4/tuto/auth/security/JwtTokenProvider.java`
- Create: `SecurityConfigTest.java`

**Step 1: Write failing test** - protected endpoint returns 401 without token

**Step 2-6: TDD cycle**

---

## Phase 3: Backend User & Conversation Modules

### Task 3.1: User Profile — GET/PATCH /api/v1/users/me

**Objective:** Get and update current user profile

**Files:**
- Read: `backend/src/main/java/spring4/tuto/user/controller/UserController.java`
- Read: `backend/src/main/java/spring4/tuto/user/service/UserService.java`
- Read: `backend/src/main/java/spring4/tuto/user/dto/UserDto.java`
- Create: `UserControllerTest.java`

**Endpoints:**
- `GET /api/v1/users/me` - returns UserDto
- `PATCH /api/v1/users/me` - update profile (username, firstName, lastName, bio, avatar)

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 3.2: User Search — GET /api/v1/users/search?q={query}

**Objective:** Search users by username, email, name for starting conversations

**Files:**
- Modify: `UserController.java`, `UserService.java`, `UserRepository.java`
- Create: `UserSearchTest.java`

**Step 1: Write failing test** - search returns paginated results

**Step 2-6: TDD cycle**

---

### Task 3.3: Block User — POST /api/v1/users/{id}/block

**Objective:** Block/unblock users, prevent messaging

**Files:**
- Read: `backend/src/main/java/spring4/tuto/user/domain/BlockedUser.java`
- Read: `backend/src/main/java/spring4/tuto/user/repository/BlockedUserRepository.java`
- Modify: `UserController.java`, `UserService.java`
- Create: `BlockUserTest.java`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 3.4: Private Conversation — POST /api/v1/conversations/private

**Objective:** Create or get existing 1:1 conversation

**Files:**
- Read: `backend/src/main/java/spring4/tuto/conversation/`
- Create: `ConversationController.java`, `ConversationService.java`, `ConversationRepository.java`
- Create: `ConversationControllerTest.java`

**Step 1: Write failing test** - create private conversation between two users

**Step 2-6: TDD cycle**

---

### Task 3.5: List Conversations — GET /api/v1/conversations

**Objective:** Paginated list of user's conversations with last message preview

**Files:**
- Modify: `ConversationController.java`, `ConversationService.java`
- Create: `ConversationListTest.java`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 3.6: Get Messages — GET /api/v1/conversations/{id}/messages

**Objective:** Cursor-based pagination for message history

**Files:**
- Read: `backend/src/main/java/spring4/tuto/message/controller/MessageController.java`
- Read: `backend/src/main/java/spring4/tuto/message/service/MessageService.java`
- Read: `backend/src/main/java/spring4/tuto/message/repository/MessageRepository.java`
- Create: `MessageControllerTest.java`

**Step 1: Write failing test** - pagination works, messages ordered by sequence_number

**Step 2-6: TDD cycle**

---

### Task 3.7: Send Message — POST /api/v1/conversations/{id}/messages

**Objective:** Create message, publish to WebSocket, return created message

**Files:**
- Modify: `MessageController.java`, `MessageService.java`
- Read: `backend/src/main/java/spring4/tuto/websocket/controller/MessageWebSocketController.java`
- Create: `SendMessageTest.java`

**Step 1: Write failing test** - message saved, WebSocket event published

**Step 2-6: TDD cycle**

---

## Phase 4: Backend Real-Time (WebSocket)

### Task 4.1: WebSocket Connection & Auth

**Objective:** STOMP over WebSocket with JWT auth handshake

**Files:**
- Read: `backend/src/main/java/spring4/tuto/websocket/config/WebSocketConfig.java`
- Read: `backend/src/main/java/spring4/tuto/websocket/config/WsChannelInterceptor.java`
- Read: `backend/src/main/java/spring4/tuto/websocket/controller/MessageWebSocketController.java`
- Create: `WebSocketConnectionTest.java`

**Step 1: Write failing test** - connect with token, subscribe to user queue

**Step 2-6: TDD cycle**

---

### Task 4.2: Message Delivery Events

**Objective:** Server → Client events: message.created, message.delivered, message.read

**Files:**
- Modify: `MessageWebSocketController.java`, `MessageService.java`
- Create: `MessageDeliveryEventsTest.java`

**Step 1: Write failing test** - send message → recipient receives message.created

**Step 2-6: TDD cycle**

---

### Task 4.3: Typing Indicators

**Objective:** typing.start / typing.stop events via Redis (not persisted)

**Files:**
- Modify: `MessageWebSocketController.java`
- Create: `TypingIndicatorTest.java`

**Redis key pattern:** `typing:{conversationId}:{userId}` with TTL

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 4.4: Presence (Online/Offline/Away)

**Objective:** Presence events via Redis, heartbeat mechanism

**Files:**
- Create: `PresenceService.java`, `PresenceController.java`
- Modify: `WebSocketConfig.java` for connect/disconnect events
- Create: `PresenceTest.java`

**Redis:** `presence:{userId}` with TTL, heartbeat every 30s

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

## Phase 5: Backend Groups & Channels

### Task 5.1: Group CRUD & Admin

**Objective:** Create group, add/remove members, promote/demote admins, permissions

**Files:**
- Read: `backend/src/main/java/spring4/tuto/group/`
- Create: `GroupController.java`, `GroupService.java`, `GroupRepository.java`
- Create: `GroupControllerTest.java`

**Endpoints:**
- `POST /api/v1/groups` - create
- `GET /api/v1/groups/{id}` - details
- `PATCH /api/v1/groups/{id}` - update (owner/admin)
- `POST /api/v1/groups/{id}/members` - add members
- `DELETE /api/v1/groups/{id}/members/{userId}` - remove
- `POST /api/v1/groups/{id}/admins/{userId}` - promote
- `DELETE /api/v1/groups/{id}/admins/{userId}` - demote

**Step 1: Write failing test** - full group lifecycle

**Step 2-6: TDD cycle**

---

### Task 5.2: Channel CRUD

**Objective:** Create channel (public/private), subscribe/unsubscribe, public username

**Files:**
- Read: `backend/src/main/java/spring4/tuto/channel/`
- Create: `ChannelController.java`, `ChannelService.java`, `ChannelRepository.java`
- Create: `ChannelControllerTest.java`

**Endpoints:**
- `POST /api/v1/channels` - create
- `GET /api/v1/channels/{id}` - details
- `POST /api/v1/channels/{id}/subscribe` - subscribe
- `DELETE /api/v1/channels/{id}/subscribe` - unsubscribe
- `GET /api/v1/channels/@{username}` - resolve public channel

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

## Phase 6: Backend Files & Media

### Task 6.1: Presigned Upload URL — POST /api/v1/files/upload-url

**Objective:** Generate S3 presigned PUT URL for direct browser upload

**Files:**
- Read: `backend/src/main/java/spring4/tuto/file/controller/FileController.java`
- Read: `backend/src/main/java/spring4/tuto/file/service/S3StorageService.java`
- Read: `backend/src/main/java/spring4/tuto/file/service/FileService.java`
- Create: `FileControllerTest.java`

**Step 1: Write failing test** - returns uploadUrl, fileId, expiresAt

**Step 2-6: TDD cycle**

---

### Task 6.2: Complete Upload — POST /api/v1/files/{id}/complete

**Objective:** Verify upload, create FileEntity, return metadata

**Files:**
- Modify: `FileController.java`, `FileService.java`
- Create: `CompleteUploadTest.java`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 6.3: Message Attachments

**Objective:** Link files to messages, store metadata (width, height, duration, thumbnail)

**Files:**
- Read: `backend/src/main/java/spring4/tuto/message/domain/MessageAttachment.java`
- Read: `backend/src/main/java/spring4/tuto/message/repository/MessageAttachmentRepository.java`
- Modify: `MessageService.java` to handle attachments
- Create: `MessageAttachmentTest.java`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

## Phase 7: Backend Notifications & Search

### Task 7.1: Notifications

**Objective:** Real-time notification events for mentions, replies, group adds, etc.

**Files:**
- Read: `backend/src/main/java/spring4/tuto/notification/`
- Create: `NotificationControllerTest.java`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 7.2: Full Text Search

**Objective:** PostgreSQL FTS for users, conversations, messages, groups, channels

**Files:**
- Create: `SearchService.java`, `SearchController.java`
- Create: `SearchTest.java`

**Endpoints:**
- `GET /api/v1/search?q={query}&type={users|conversations|messages|groups|channels}`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

## Phase 8: Frontend Foundation (Next.js 15)

### Task 8.1: Project Configuration & Tailwind + Frequency Tokens

**Objective:** Configure Next.js, Tailwind with Frequency CSS variables, shadcn/ui

**Files:**
- Create: `frontend/next.config.ts`
- Create: `frontend/tailwind.config.ts`
- Create: `frontend/app/globals.css` — Frequency CSS variables
- Create: `frontend/components/ui/` — shadcn components

**Step 1: Write failing test** - tailwind config parses with Frequency colors

**Step 2: Implement** - per frontend PRD §5.3, implementation plan Task 1.1-1.2

**Step 3: Commit**

---

### Task 8.2: Theme Provider & Frequency System

**Objective:** ThemeProvider with 6 curated Frequencies + custom Theme Studio

**Files:**
- Create: `frontend/lib/theme/tokens.ts` — Frequency definitions
- Create: `frontend/lib/theme/ThemeProvider.tsx`
- Create: `frontend/components/theme/FrequencySwitcher.tsx`
- Create: `frontend/components/theme/ThemeStudio.tsx`
- Create: `frontend/lib/stores/useThemeStore.ts` (Zustand + persist)

**Step 1: Write failing test** - ThemeProvider applies Frequency CSS vars

**Step 2-6: TDD cycle**

---

### Task 8.3: Auth Provider & Routing

**Objective:** JWT session management, protected routes, auth pages

**Files:**
- Create: `frontend/lib/auth/provider.tsx` (AuthProvider)
- Create: `frontend/lib/auth/hooks.ts` (useAuth)
- Create: `frontend/app/(auth)/layout.tsx`
- Create: `frontend/app/(app)/layout.tsx`
- Create: `frontend/components/auth/LoginForm.tsx`
- Create: `frontend/components/auth/RegisterForm.tsx`
- Create: `frontend/app/(auth)/login/page.tsx`
- Create: `frontend/app/(auth)/register/page.tsx`
- Create: `frontend/app/(auth)/verify/page.tsx`
- Create: `frontend/app/(auth)/forgot-password/page.tsx`

**Step 1: Write failing test** - AuthProvider persists session to localStorage

**Step 2-6: TDD cycle**

---

### Task 8.4: API Client & TanStack Query Setup

**Objective:** Typed REST client with TanStack Query hooks

**Files:**
- Create: `frontend/lib/api/client.ts`
- Create: `frontend/lib/api/hooks.ts` (useAuth, useUsers, useConversations, etc.)
- Create: `frontend/lib/api/types.ts`

**Step 1: Write failing test** - useUsersMe() returns user data

**Step 2-6: TDD cycle**

---

### Task 8.5: WebSocket Client (Native JSON Protocol)

**Objective:** Custom WebSocket client with reconnection, idempotency, event bus

**Files:**
- Create: `frontend/lib/ws/client.ts` (WebSocketClient class)
- Create: `frontend/lib/ws/types.ts` (ClientEvent, ServerEvent)
- Create: `frontend/lib/ws/hooks.ts` (useWebSocket)
- Modify: `frontend/lib/stores/useConversationStore.ts` (integrate WS)
- Create: `frontend/lib/ws/__tests__/client.test.ts`

**Protocol (per PRD §9):**
- Client→Server: message.send, typing.start, typing.stop, conversation.read, reaction.add/remove
- Server→Client: message.created, message.delivered, message.read, typing.started/stopped, presence.changed, conversation.updated

**Step 1: Write failing test** - WebSocketClient connects, sends, receives

**Step 2-6: TDD cycle**

---

## Phase 9: Frontend Core Messenger UI

### Task 9.1: Three-Zone Layout (App Shell)

**Objective:** Responsive shell: Rail + Conversation List + Chat Panel + Details Panel

**Files:**
- Create: `frontend/app/(app)/layout.tsx`
- Create: `frontend/components/chat/ConversationList.tsx`
- Create: `frontend/components/chat/MessageList.tsx`
- Create: `frontend/components/chat/ChatPanel.tsx`
- Create: `frontend/components/chat/DetailsPanel.tsx`
- Create: `frontend/components/chat/Composer.tsx`

**Breakpoints:** Desktop (≥1280px), Tablet (768-1279px), Mobile (<768px)

**Step 1: Write failing test** - layout renders three zones

**Step 2-6: TDD cycle**

---

### Task 9.2: Conversation List with Pulse Rail

**Objective:** List conversations with activity waveform rail (Pulse Rail)

**Files:**
- Create: `frontend/components/chat/PulseRail.tsx`
- Modify: `ConversationList.tsx`
- Create: `PulseRail.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 9.3: Message Lifecycle (Optimistic → SENT → DELIVERED → READ)

**Objective:** MessageBubble with status badges, optimistic rendering, reconciliation

**Files:**
- Create: `frontend/components/chat/MessageBubble.tsx`
- Modify: `frontend/lib/stores/useMessageStore.ts`
- Modify: `Composer.tsx` — onSubmit triggers WS send + optimistic message
- Create: `MessageBubble.test.tsx`
- Create: `MessageLifecycle.test.tsx`

**Status flow:**
1. User sends → optimistic bubble (SENDING) + Undo Send toast (4s)
2. WS message.created → SENT (single tick)
3. WS message.delivered → DELIVERED (double tick)
4. WS message.read → READ (double tick, accent color)
5. On failure → FAILED + Retry button

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 9.4: Typing Indicator (Pulse Motif)

**Objective:** Show typing Pulse in conversation header, debounced

**Files:**
- Create: `frontend/components/chat/TypingPulse.tsx`
- Modify: `ChatPanel.tsx`
- Modify: `useConversationStore.ts` (handle typing events)
- Create: `TypingPulse.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 9.5: Presence Indicators

**Objective:** Online/offline/away badges in conversation list and chat header

**Files:**
- Modify: `ConversationList.tsx`, `ChatPanel.tsx`
- Modify: `useConversationStore.ts` (handle presence.changed)
- Create: `PresenceIndicator.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 9.6: Composer with Media Upload

**Objective:** Text input + attach files/images → presigned upload → send message

**Files:**
- Modify: `Composer.tsx`
- Create: `frontend/components/chat/MediaPicker.tsx`
- Create: `frontend/hooks/useFileUpload.ts`
- Create: `Composer.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 9.7: Reply, Edit, Delete Messages

**Objective:** Message actions: reply preview, edit (with edited_at), delete (soft)

**Files:**
- Create: `frontend/components/chat/MessageActions.tsx`
- Modify: `MessageBubble.tsx`
- Modify: `useMessageStore.ts`
- Create: `MessageActions.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 9.8: In-Conversation Search

**Objective:** Search messages within open conversation (local IndexedDB + backend fallback)

**Files:**
- Create: `frontend/components/chat/ConversationSearch.tsx`
- Create: `frontend/hooks/useConversationSearch.ts`
- Create: `ConversationSearch.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

## Phase 10: Frontend Advanced Features

### Task 10.1: Command Palette (⌘K / Ctrl+K)

**Objective:** Global command palette for navigation and actions

**Files:**
- Create: `frontend/components/ui/CommandPalette.tsx`
- Create: `frontend/hooks/useCommandPalette.ts`
- Create: `frontend/lib/commands/` (registry of commands)
- Create: `CommandPalette.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 10.2: Undo Send

**Objective:** 4s grace period after send with Undo toast

**Files:**
- Modify: `Composer.tsx`, `useMessageStore.ts`
- Create: `UndoSend.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 10.3: Draft Continuity Across Devices

**Objective:** Persist composer drafts per conversation, sync via backend

**Files:**
- Create: `frontend/hooks/useDraftSync.ts`
- Modify: `Composer.tsx`
- Backend: Add draft endpoint
- Create: `DraftSync.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 10.4: Split View & Popout

**Objective:** Pin second conversation side-by-side (ultra-wide), popout window

**Files:**
- Create: `frontend/components/chat/SplitView.tsx`
- Create: `frontend/components/chat/PopoutWindow.tsx`
- Modify: `app/(app)/layout.tsx`
- Create: `SplitView.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 10.5: Focus Hours (Notification Intelligence)

**Objective:** Quiet windows, bundle non-mention notifications, digest

**Files:**
- Create: `frontend/lib/notifications/FocusHours.ts`
- Create: `frontend/components/notifications/NotificationCenter.tsx`
- Create: `FocusHours.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 10.6: Granular Read Receipt Privacy

**Objective:** Per-conversation read receipt toggle

**Files:**
- Create: `frontend/components/settings/PrivacySettings.tsx`
- Backend: Add preference to ConversationMember
- Create: `ReadReceiptPrivacy.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

## Phase 11: Frontend Accessibility & Polish

### Task 11.1: Full Accessibility Baseline

**Objective:** WCAG 2.2 AA, keyboard operability, visible focus, no color-only status

**Files:**
- All components: add ARIA, focus management
- Create: `frontend/components/ui/AccessibleComponents.tsx`
- Configure: axe-core in CI
- Create: `a11y.test.tsx`

**Step 1: Write failing test** - axe-core passes on key pages

**Step 2-6: TDD cycle**

---

### Task 11.2: Dyslexia-Friendly Font & Text Scale

**Objective:** Toggle Atkinson Hyperlegible, independent text-scale slider

**Files:**
- Modify: `globals.css`, `ThemeProvider.tsx`
- Create: `frontend/components/theme/AccessibilitySettings.tsx`
- Create: `AccessibilitySettings.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 11.3: Responsive Breakpoints & Touch

**Objective:** All screens work on Desktop/Tablet/Mobile per PRD §7

**Files:**
- All layout components
- Test: Playwright E2E on multiple viewports
- Create: `responsive.test.ts`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

## Phase 12: Frontend Settings & Admin

### Task 12.1: Settings Sections

**Objective:** Account, Privacy, Notifications, Sessions, Appearance (Theme Studio), Language, Security

**Files:**
- Create: `frontend/app/(app)/settings/` (all 7 sections)
- Create: `frontend/components/settings/` (shared components)
- Create: `Settings.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

### Task 12.2: Admin Surface

**Objective:** Separate data-dense admin route group (not themed with Frequencies)

**Files:**
- Create: `frontend/app/(admin)/` (Users, Groups, Channels, Messages, Reports, Moderation, Files, Sessions, System Health, Audit Logs)
- Create: `Admin.test.tsx`

**Step 1: Write failing test**

**Step 2-6: TDD cycle**

---

## Phase 13: Testing & CI/CD

### Task 13.1: Frontend Unit/Component Tests

**Objective:** Vitest + RTL for all components, MSW for API mocking

**Files:**
- Configure: `frontend/vitest.config.ts`
- Create: `__mocks__/handlers.ts` (MSW)
- Create: tests for all components above

**Step 1: Run** `npm test` — all pass

**Step 2: Commit**

---

### Task 13.2: Frontend E2E Tests (Playwright)

**Objective:** Critical user paths per PRD §15

**Files:**
- Configure: `frontend/playwright.config.ts`
- Create: `frontend/e2e/` (11 critical paths)

**Critical paths:**
1. Register → verify email → login
2. Find user → start conversation → send/receive message
3. See delivery/read state update live
4. See typing indicator live
5. Send image
6. Create group → manage it
7. Search within conversation
8. Log out → log in from different browser
9. Switch Frequencies
10. Build custom theme in Theme Studio
11. Use command palette → recover queued message after network drop

**Step 1: Write failing tests**

**Step 2: Implement features to pass**

**Step 3: Run** `npm run e2e` — all pass

**Step 4: Commit**

---

### Task 13.3: Storybook Documentation

**Objective:** Document all UI components in Storybook

**Files:**
- Configure: `frontend/.storybook/`
- Create: stories for all components

**Step 1: Run** `npm run storybook` — no errors

**Step 2: Commit**

---

### Task 13.4: GitHub Actions CI

**Objective:** Automated verification on every push

**Files:**
- Create: `.github/workflows/ci-backend.yml`
- Create: `.github/workflows/ci-frontend.yml`

**Backend CI:**
```yaml
- Checkout
- Setup Java 21
- Start Docker (PostgreSQL, Redis, MinIO)
- ./mvnw verify
- Docker build
```

**Frontend CI:**
```yaml
- Checkout
- Setup Node 22
- npm ci
- npm run lint
- npm run test:ci
- npm run e2e
- npm run build
```

**Step 1: Write workflow files**

**Step 2: Push and verify CI passes**

---

## Phase 14: Integration & End-to-End Verification

### Task 14.1: Full Stack Integration Test

**Objective:** Run backend + frontend together, verify all MVP flows

**Files:**
- Create: `docker-compose.full.yml` (backend + frontend + infra)
- Create: integration test script

**Step 1: Start full stack**
```bash
docker compose -f docker-compose.full.yml up -d
```

**Step 2: Run Playwright E2E against full stack**

**Step 3: Verify all 11 critical paths**

**Step 4: Commit**

---

### Task 14.2: Performance & Accessibility Budgets

**Objective:** Meet PRD §11 targets

| Metric | Target |
|--------|--------|
| LCP | < 2.0s |
| INP | < 200ms |
| CLS | < 0.1 |
| Message render after WS | < 100ms |
| Frequency switch | < 1 frame |
| Accessibility | WCAG 2.2 AA |

**Files:**
- Configure: Lighthouse CI, Web Vitals
- Create: `performance.test.ts`

**Step 1: Run audits**

**Step 2: Optimize if needed**

**Step 3: Commit**

---

## Phase 15: Documentation & Handoff

### Task 15.1: API Documentation (OpenAPI/Swagger)

**Objective:** Complete OpenAPI spec for all endpoints

**Files:**
- Verify: `backend/springdoc-openapi` config
- Enhance: DTOs with `@Schema` annotations
- Generate: OpenAPI JSON

**Step 1: Verify Swagger UI complete**

**Step 2: Commit**

---

### Task 15.2: Architecture Diagrams

**Objective:** Visual documentation of system architecture

**Files:**
- Create: `docs/architecture.html` (using architecture-diagram skill)
- Create: `docs/sequence-diagrams.excalidraw`

**Step 1: Generate diagrams**

**Step 2: Commit**

---

### Task 15.3: Developer Onboarding Docs

**Objective:** README, CONTRIBUTING, setup guides

**Files:**
- Update: `backend/README.md`, `frontend/README.md`
- Create: `CONTRIBUTING.md`
- Create: `DEVELOPMENT.md`

**Step 1: Write docs**

**Step 2: Commit**

---

## Verification Commands Summary

### Backend
```bash
cd backend
./mvnw test                    # Unit + integration tests
./mvnw verify                  # Full verification (tests, checkstyle, etc.)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  # Dev server
docker compose -f docker-compose.dev.yml up -d         # Infrastructure
```

### Frontend
```bash
cd frontend
npm run dev                    # Dev server on :3000
npm run build                  # Production build
npm run lint                   # ESLint
npm test                       # Vitest unit tests
npm run test:ci                # Vitest with coverage
npm run e2e                    # Playwright E2E
npm run storybook              # Storybook
```

### Full Stack
```bash
# Terminal 1: Backend
cd backend && docker compose -f docker-compose.dev.yml up -d && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Terminal 2: Frontend
cd frontend && npm run dev

# Terminal 3: E2E
cd frontend && npm run e2e
```

---

## Risks & Tradeoffs

| Risk | Mitigation |
|------|------------|
| WebSocket reconnection complexity | Use proven exponential backoff, test with network throttling |
| Optimistic UI reconciliation edge cases | Comprehensive test matrix: send, retry, reconnect, concurrent edits |
| Frequency theming CSS variable conflicts | Strict token system, test all 6 Frequencies + custom |
| Mobile responsive breakpoints | Test on real devices + Playwright device emulation |
| Accessibility regressions | axe-core in CI on every PR |
| Testcontainers flakiness | Use Ryuk, stable Docker, retry logic |
| Frontend dependency conflicts (React 19) | Use --legacy-peer-deps, pin compatible testing-library versions |

---

## Open Questions

1. **Email service:** Use Mock/Mailpit for dev, real SMTP for prod?
2. **Push notifications:** Web Push API (VAPID) — implement in MVP or V2?
3. **File upload size limits:** Configure in backend (Spring) and MinIO?
4. **Rate limiting:** Apply to auth endpoints? Which library?
5. **Internationalization:** i18next namespaces per feature — which languages first?
6. **Admin surface authentication:** Separate admin JWT claims or role-based?

---

## Success Criteria (from PRD §15)

Two real users, from browser alone, can:
1. Create account, verify email, log in
2. Find each other, start conversation
3. Send/receive message with no manual refresh
4. See delivery and read state update live
5. See typing indicator live
6. Send an image
7. Create a group, manage it
8. Search within it
9. Log out, log back in from different browser
10. Switch Frequencies
11. Build custom theme in Theme Studio
12. Use command palette to navigate
13. Recover queued message after simulated network drop

**All without the interface ever feeling like it's fighting them.**

---

## Execution Strategy

1. **Phase 0-1** (Infrastructure): Complete — verified working
2. **Phase 2-7** (Backend MVP): Implement using strict TDD, one endpoint at a time
3. **Phase 8-10** (Frontend Core): Build shell, auth, real-time, messenger UI
4. **Phase 11-12** (Advanced + Settings): Differentiators, accessibility, admin
5. **Phase 13** (Testing): Unit, E2E, Storybook, CI
6. **Phase 14** (Integration): Full stack verification
7. **Phase 15** (Documentation): Diagrams, docs, handoff

**Each task follows TDD:** Write failing test → Run (fail) → Minimal impl → Run (pass) → Refactor → Commit

**Quality gates per requesting-code-review:** Security scan → Baseline tests → Independent reviewer → Auto-fix loop → Commit with `[verified]`