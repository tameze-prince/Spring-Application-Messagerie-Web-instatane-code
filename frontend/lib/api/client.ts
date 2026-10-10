/** Typed REST client boundary. Set NEXT_PUBLIC_API_URL to your Spring API origin. */
const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly fieldErrors?: Record<string, string>,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

const UUID_PATTERN = /^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i;
const USERNAME_PATTERN = /^[A-Za-z0-9._-]+$/;
const CONTROL_CHAR_PATTERN = /[\u0000-\u001F\u007F-\u009F]/;
const EMAIL_PATTERN = /^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$/;

function validateEmail(email: string): void {
  const normalized = email.trim();
  if (!normalized || normalized.length > 255 || !EMAIL_PATTERN.test(normalized)) {
    invalidInput("Enter a valid email address (maximum 255 characters).");
  }
}

function validatePassword(password: string): void {
  if (!password.trim()) invalidInput("Password is required.");
  if (password.length < 8 || password.length > 128) {
    invalidInput("Password must be between 8 and 128 characters.");
  }
}

function validateRegistration(data: RegisterRequest): RegisterRequest {
  validateRequiredText(data.username, "Username", 3, 50);
  if (!USERNAME_PATTERN.test(data.username.trim())) {
    invalidInput("Username may contain only letters, numbers, dots, underscores and hyphens.");
  }
  validateEmail(data.email);
  validatePassword(data.password);
  validateOptionalText(data.firstName, "First name", 80);
  validateOptionalText(data.lastName, "Last name", 80);
  return {
    ...data,
    username: data.username.trim(),
    email: data.email.trim().toLowerCase(),
    firstName: data.firstName?.trim() || undefined,
    lastName: data.lastName?.trim() || undefined,
  };
}

function validateLogin(data: LoginRequest): LoginRequest {
  validateRequiredText(data.login, "Email or username", 1, 255);
  validatePassword(data.password);
  return { ...data, login: data.login.trim() };
}

function validateRefreshToken(refreshToken: string): string {
  validateRequiredText(refreshToken, "Refresh token", 1, 4096);
  return refreshToken.trim();
}


function invalidInput(message: string): never {
  throw new ApiError(400, message);
}

function validateRequiredText(value: string, label: string, minLength: number, maxLength: number): void {
  if (!value.trim()) invalidInput(`${label} is required.`);
  if (value.length < minLength || value.length > maxLength) {
    invalidInput(`${label} must be between ${minLength} and ${maxLength} characters.`);
  }
  if (CONTROL_CHAR_PATTERN.test(value)) invalidInput(`${label} contains invalid characters.`);
}

function validateOptionalText(value: string | undefined, label: string, maxLength: number): void {
  if (value === undefined) return;
  if (value.length > maxLength) invalidInput(`${label} must not exceed ${maxLength} characters.`);
  if (CONTROL_CHAR_PATTERN.test(value)) invalidInput(`${label} contains invalid characters.`);
}

function validateUuid(value: string | undefined, label: string, required = false): void {
  if (value === undefined) {
    if (required) invalidInput(`${label} is required.`);
    return;
  }
  if (!UUID_PATTERN.test(value)) invalidInput(`${label} must be a valid UUID.`);
}

function validateGroupPayload(data: { name?: string; description?: string; avatarFileId?: string; maxMembers?: number; joinPolicy?: string }, required: boolean): void {
  if (data.name !== undefined) validateRequiredText(data.name, "Group name", 1, 80);
  else if (required) invalidInput("Group name is required.");
  validateOptionalText(data.description, "Group description", 500);
  validateUuid(data.avatarFileId, "Avatar file ID");
  if (data.maxMembers !== undefined && (!Number.isInteger(data.maxMembers) || data.maxMembers < 2 || data.maxMembers > 500)) {
    invalidInput("Group size must be between 2 and 500 members.");
  }
  if (data.joinPolicy !== undefined && !["EVERYONE", "ADMINS_ONLY"].includes(data.joinPolicy)) {
    invalidInput("Join policy must be EVERYONE or ADMINS_ONLY.");
  }
}

function validateChannelPayload(data: { name?: string; username?: string; description?: string; avatarFileId?: string; visibility?: string; postPermission?: string }, required: boolean): void {
  if (data.name !== undefined) validateRequiredText(data.name, "Channel name", 1, 80);
  else if (required) invalidInput("Channel name is required.");
  if (data.username !== undefined) {
    if (data.username.length < 3 || data.username.length > 50 || !USERNAME_PATTERN.test(data.username)) {
      invalidInput("Channel username must be 3–50 characters using letters, numbers, dots, underscores or hyphens.");
    }
  } else if (required) invalidInput("Channel username is required.");
  validateOptionalText(data.description, "Channel description", 500);
  validateUuid(data.avatarFileId, "Avatar file ID");
  if (data.visibility !== undefined && !["PUBLIC", "PRIVATE"].includes(data.visibility)) {
    invalidInput("Visibility must be PUBLIC or PRIVATE.");
  }
  if (data.postPermission !== undefined && !["EVERYONE", "ADMINS_ONLY"].includes(data.postPermission)) {
    invalidInput("Post permission must be EVERYONE or ADMINS_ONLY.");
  }
}

function validateMessageBody(body: string): void {
  if (!body.trim()) invalidInput("Message cannot be empty.");
  if (body.length > 4000) invalidInput("Message must not exceed 4000 characters.");
}

function validateReaction(reaction: string): void {
  if (!reaction.trim()) invalidInput("Reaction cannot be empty.");
  if (reaction.trim().length > 32) invalidInput("Reaction must not exceed 32 characters.");
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("accessToken") : null;
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...init,
      headers: {
        "Content-Type": "application/json",
        ...(token ? { "Authorization": `Bearer ${token}` } : {}),
        ...init.headers,
      },
      credentials: "include",
    });
  } catch {
    throw new ApiError(
      0,
      "Unable to reach the API. Check the backend URL and cross-origin access configuration.",
    );
  }

  if (!response.ok) {
    const raw = await response.text();
    let message = raw || "Request failed";
    let fieldErrors: Record<string, string> | undefined;

    try {
      const payload = JSON.parse(raw) as { message?: string; data?: unknown };
      message = payload.message || message;
      if (payload.data && typeof payload.data === "object" && !Array.isArray(payload.data)) {
        const entries = Object.entries(payload.data as Record<string, unknown>)
          .filter((entry): entry is [string, string] => typeof entry[1] === "string");
        if (entries.length > 0) fieldErrors = Object.fromEntries(entries);
      }
    } catch {
      // Keep the raw response text when the server did not return JSON.
    }

    throw new ApiError(response.status, message, fieldErrors);
  }

  return response.status === 204 ? (undefined as T) : response.json() as Promise<T>;
}

// Auth
export type RegisterRequest = { username: string; email: string; password: string; firstName?: string; lastName?: string };
export type LoginRequest = { login: string; password: string };
export type RefreshTokenRequest = { refreshToken: string };
export type AuthResponse = { accessToken: string; refreshToken: string; tokenType: string; user: UserDto };

export const authApi = {
  register: (data: RegisterRequest) => api<{ success: boolean; data: AuthResponse }>("/auth/register", { method: "POST", body: JSON.stringify(validateRegistration(data)) }),
  login: (data: LoginRequest) => api<{ success: boolean; data: AuthResponse }>("/auth/login", { method: "POST", body: JSON.stringify(validateLogin(data)) }),
  refresh: (data: RefreshTokenRequest) => api<{ success: boolean; data: AuthResponse }>("/auth/refresh", { method: "POST", body: JSON.stringify({ refreshToken: validateRefreshToken(data.refreshToken) }) }),
  logout: (refreshToken: string) => api<{ success: boolean; data: null }>("/auth/logout", { method: "POST", body: JSON.stringify({ refreshToken: validateRefreshToken(refreshToken) }) }),
};

// Users
export type UserDto = { id: string; username: string; email: string; firstName?: string; lastName?: string; bio?: string; avatarFileId?: string; status?: string };
export type UpdateProfileRequest = { firstName?: string; lastName?: string; bio?: string; avatarFileId?: string };

export const userApi = {
  me: () => api<{ success: boolean; data: UserDto }>("/users/me"),
  updateProfile: (data: UpdateProfileRequest) => {
    validateOptionalText(data.firstName, "First name", 80);
    validateOptionalText(data.lastName, "Last name", 80);
    validateOptionalText(data.bio, "Bio", 500);
    validateUuid(data.avatarFileId, "Avatar file ID");
    return api<{ success: boolean; data: UserDto }>("/users/me", { method: "PATCH", body: JSON.stringify(data) });
  },
  getById: (id: string) => api<{ success: boolean; data: UserDto }>(`/users/${id}`),
  search: (q: string) => {
    const normalizedQuery = q.trim();
    if (normalizedQuery.length > 100) {
      throw new ApiError(400, "Search query must not exceed 100 characters.");
    }
    return api<{ success: boolean; data: UserDto[] }>(`/users/search?q=${encodeURIComponent(normalizedQuery)}`);
  },
  block: (id: string) => api<{ success: boolean; data: null }>(`/users/${id}/block`, { method: "POST" }),
  unblock: (id: string) => api<{ success: boolean; data: null }>(`/users/${id}/block`, { method: "DELETE" }),
};

// Conversations
export type ConversationDto = { 
  id: string; 
  type: string; 
  title?: string; 
  username?: string; 
  description?: string; 
  avatarFileId?: string; 
  ownerId?: string; 
  createdAt?: string; 
  updatedAt?: string;
  lastMessageBody?: string;
  lastMessageAt?: string;
};

export type CreatePrivateConversationRequest = { targetUserId: string };

export const conversationApi = {
  list: () => api<{ success: boolean; data: ConversationDto[] }>("/conversations"),
  get: (id: string) => api<{ success: boolean; data: ConversationDto }>(`/conversations/${id}`),
  createPrivate: (data: CreatePrivateConversationRequest) => {
    validateUuid(data.targetUserId, "Target user ID", true);
    return api<{ success: boolean; data: ConversationDto }>("/conversations/private", { method: "POST", body: JSON.stringify(data) });
  },
};

// Messages
export type SendMessageInput = { clientMessageId: string; conversationId: string; content: string; replyToId?: string };
export type MessageReceipt = "SENDING" | "SENT" | "DELIVERED" | "READ" | "FAILED";

export type MessageDto = { 
  id: string; 
  conversationId: string; 
  senderId: string; 
  type: string; 
  body: string; 
  replyToMessageId?: string; 
  editedAt?: string; 
  deletedAt?: string; 
  createdAt: string; 
  updatedAt: string; 
  sequenceNumber: number;
  reactions?: string[];
  sender?: UserDto;
};

export type SendMessageRequest = { type?: string; body: string; replyToMessageId?: string };
export type EditMessageRequest = { body: string };
export type ReactionRequest = { reaction: string };

export const messageApi = {
  list: (conversationId: string, page = 0, size = 30) => {
    if (!Number.isInteger(page) || page < 0) invalidInput("Page index must be zero or greater.");
    if (!Number.isInteger(size) || size < 1 || size > 100) invalidInput("Page size must be between 1 and 100.");
    return api<{ success: boolean; data: MessageDto[] }>(`/conversations/${conversationId}/messages?page=${page}&size=${size}`);
  },
  send: (conversationId: string, data: SendMessageRequest) => {
    validateMessageBody(data.body);
    validateOptionalText(data.type, "Message type", 50);
    validateUuid(data.replyToMessageId, "Reply message ID");
    return api<{ success: boolean; data: MessageDto }>(`/conversations/${conversationId}/messages`, { method: "POST", body: JSON.stringify(data) });
  },
  edit: (messageId: string, data: EditMessageRequest) => {
    validateMessageBody(data.body);
    return api<{ success: boolean; data: MessageDto }>(`/messages/${messageId}`, { method: "PATCH", body: JSON.stringify(data) });
  },
  delete: (messageId: string) => api<{ success: boolean; data: null }>(`/messages/${messageId}`, { method: "DELETE" }),
  addReaction: (messageId: string, data: ReactionRequest) => {
    validateReaction(data.reaction);
    return api<{ success: boolean; data: null }>(`/messages/${messageId}/reaction`, { method: "POST", body: JSON.stringify({ reaction: data.reaction.trim() }) });
  },
  removeReaction: (messageId: string, reaction: string) => {
    validateReaction(reaction);
    return api<{ success: boolean; data: null }>(`/messages/${messageId}/reaction?reaction=${encodeURIComponent(reaction.trim())}`, { method: "DELETE" });
  },
};

// Groups
export type GroupDto = {
  id: string;
  title: string;
  description?: string;
  avatarFileId?: string;
  type: string;
  ownerId?: string;
  maxMembers?: number;
  joinPolicy?: string;
  approvalRequired?: boolean;
  createdAt?: string;
  updatedAt?: string;
  members?: GroupMemberInfo[];
};

export type GroupMemberInfo = { userId: string; username?: string; displayName?: string; avatarFileId?: string; role: string; status: string; joinedAt?: string };

export type CreateGroupRequest = { name: string; description?: string; avatarFileId?: string; maxMembers?: number; joinPolicy?: string; approvalRequired?: boolean; initialMemberIds?: string[] };
export type UpdateGroupRequest = { name?: string; description?: string; avatarFileId?: string; maxMembers?: number; joinPolicy?: string; approvalRequired?: boolean };
export type AddMemberRequest = { userId: string };

export const groupApi = {
  create: (data: CreateGroupRequest) => {
    validateGroupPayload(data, true);
    if ((data.initialMemberIds?.length ?? 0) > 199) invalidInput("A group can include at most 199 initial members.");
    data.initialMemberIds?.forEach((id) => validateUuid(id, "Initial member ID", true));
    return api<{ success: boolean; data: GroupDto }>("/groups", { method: "POST", body: JSON.stringify(data) });
  },
  get: (id: string) => api<{ success: boolean; data: GroupDto }>(`/groups/${id}`),
  update: (id: string, data: UpdateGroupRequest) => {
    validateGroupPayload(data, false);
    return api<{ success: boolean; data: GroupDto }>(`/groups/${id}`, { method: "PATCH", body: JSON.stringify(data) });
  },
  delete: (id: string) => api<{ success: boolean; data: null }>(`/groups/${id}`, { method: "DELETE" }),
  getMembers: (id: string) => api<{ success: boolean; data: GroupMemberInfo[] }>(`/groups/${id}/members`),
  addMember: (id: string, data: AddMemberRequest) => {
    validateUuid(data.userId, "User ID", true);
    return api<{ success: boolean; data: null }>(`/groups/${id}/members`, { method: "POST", body: JSON.stringify(data) });
  },
  removeMember: (id: string, userId: string) => api<{ success: boolean; data: null }>(`/groups/${id}/members/${userId}`, { method: "DELETE" }),
  promoteToAdmin: (id: string, userId: string) => api<{ success: boolean; data: null }>(`/groups/${id}/admins/${userId}`, { method: "POST" }),
  demoteFromAdmin: (id: string, userId: string) => api<{ success: boolean; data: null }>(`/groups/${id}/admins/${userId}`, { method: "DELETE" }),
};

// Channels
export type ChannelDto = {
  id: string;
  title: string;
  username: string;
  description?: string;
  avatarFileId?: string;
  type: string;
  ownerId?: string;
  visibility?: string;
  postPermission?: string;
  subscriberCount?: number;
  createdAt?: string;
  updatedAt?: string;
  subscribers?: ChannelSubscriberInfo[];
};

export type ChannelSubscriberInfo = { userId: string; username?: string; displayName?: string; avatarFileId?: string; role: string; status: string; joinedAt?: string };

export type CreateChannelRequest = { name: string; username: string; description?: string; avatarFileId?: string; visibility?: string; postPermission?: string };
export type UpdateChannelRequest = { name?: string; username?: string; description?: string; avatarFileId?: string; visibility?: string; postPermission?: string };

export const channelApi = {
  create: (data: CreateChannelRequest) => {
    validateChannelPayload(data, true);
    return api<{ success: boolean; data: ChannelDto }>("/channels", { method: "POST", body: JSON.stringify(data) });
  },
  get: (id: string) => api<{ success: boolean; data: ChannelDto }>(`/channels/${id}`),
  getByUsername: (username: string) => {
    if (username.length < 3 || username.length > 50 || !USERNAME_PATTERN.test(username)) {
      invalidInput("Channel username must be 3–50 characters using letters, numbers, dots, underscores or hyphens.");
    }
    return api<{ success: boolean; data: ChannelDto }>(`/channels/username/${encodeURIComponent(username)}`);
  },
  getPublic: () => api<{ success: boolean; data: ChannelDto[] }>("/channels/public"),
  update: (id: string, data: UpdateChannelRequest) => {
    validateChannelPayload(data, false);
    return api<{ success: boolean; data: ChannelDto }>(`/channels/${id}`, { method: "PATCH", body: JSON.stringify(data) });
  },
  delete: (id: string) => api<{ success: boolean; data: null }>(`/channels/${id}`, { method: "DELETE" }),
  getSubscribers: (id: string) => api<{ success: boolean; data: ChannelSubscriberInfo[] }>(`/channels/${id}/subscribers`),
  subscribe: (id: string) => api<{ success: boolean; data: null }>(`/channels/${id}/subscribe`, { method: "POST" }),
  unsubscribe: (id: string) => api<{ success: boolean; data: null }>(`/channels/${id}/subscribe`, { method: "DELETE" }),
};

// Files
export type FileDto = { id: string; ownerId: string; storageProvider: string; storageKey: string; originalName: string; mimeType: string; sizeBytes: number; checksum?: string; status: string; createdAt: string };

export type UploadUrlResponse = { fileId: string; uploadUrl: string; storageKey: string };

export const fileApi = {
  requestUploadUrl: (filename: string, mimeType: string, sizeBytes: number) => {
    validateRequiredText(filename, "Filename", 1, 255);
    if (filename.includes("/") || filename.includes("\\")) invalidInput("Filename cannot contain path separators.");
    validateRequiredText(mimeType, "MIME type", 1, 127);
    if (!Number.isInteger(sizeBytes) || sizeBytes < 1 || sizeBytes > 50 * 1024 * 1024) {
      invalidInput("File size must be between 1 byte and 50 MiB.");
    }
    return api<{ success: boolean; data: UploadUrlResponse }>("/files/upload-url", { method: "POST", body: JSON.stringify({ filename: filename.trim(), mimeType: mimeType.trim(), sizeBytes }) });
  },
  completeUpload: (fileId: string, checksum: string) => {
    validateUuid(fileId, "File ID", true);
    validateRequiredText(checksum, "Checksum", 1, 255);
    return api<{ success: boolean; data: FileDto }>(`/files/${fileId}/complete`, { method: "POST", body: JSON.stringify({ checksum: checksum.trim() }) });
  },
  get: (fileId: string) => {
    validateUuid(fileId, "File ID", true);
    return api<{ success: boolean; data: FileDto }>(`/files/${fileId}`);
  },
  delete: (fileId: string) => {
    validateUuid(fileId, "File ID", true);
    return api<{ success: boolean; data: null }>(`/files/${fileId}`, { method: "DELETE" });
  },
};

// Notifications
export type NotificationDto = { id: string; userId: string; type: string; title: string; body: string; data?: Record<string, unknown>; readAt?: string; createdAt: string };

export const notificationApi = {
  list: () => api<{ success: boolean; data: NotificationDto[] }>("/notifications"),
  markRead: (id: string) => api<{ success: boolean; data: null }>(`/notifications/${id}/read`, { method: "PATCH" }),
};
