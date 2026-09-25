/** Typed REST client boundary. Set NEXT_PUBLIC_API_URL to your Spring API origin. */
const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) { super(message); }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("accessToken") : null;
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: { 
      "Content-Type": "application/json", 
      ...(token ? { "Authorization": `Bearer ${token}` } : {}),
      ...init.headers 
    },
    credentials: "include",
  });
  if (!response.ok) throw new ApiError(response.status, (await response.text()) || "Request failed");
  return response.status === 204 ? (undefined as T) : response.json() as Promise<T>;
}

// Auth
export type RegisterRequest = { username: string; email: string; password: string; firstName?: string; lastName?: string };
export type LoginRequest = { login: string; password: string };
export type RefreshTokenRequest = { refreshToken: string };
export type AuthResponse = { accessToken: string; refreshToken: string; tokenType: string; user: UserDto };

export const authApi = {
  register: (data: RegisterRequest) => api<{ success: boolean; data: AuthResponse }>("/auth/register", { method: "POST", body: JSON.stringify(data) }),
  login: (data: LoginRequest) => api<{ success: boolean; data: AuthResponse }>("/auth/login", { method: "POST", body: JSON.stringify(data) }),
  refresh: (data: RefreshTokenRequest) => api<{ success: boolean; data: AuthResponse }>("/auth/refresh", { method: "POST", body: JSON.stringify(data) }),
  logout: (refreshToken: string) => api<{ success: boolean; data: null }>("/auth/logout", { method: "POST", body: JSON.stringify({ refreshToken }) }),
};

// Users
export type UserDto = { id: string; username: string; email: string; firstName?: string; lastName?: string; bio?: string; avatarFileId?: string; status?: string };
export type UpdateProfileRequest = { firstName?: string; lastName?: string; bio?: string; avatarFileId?: string };

export const userApi = {
  me: () => api<{ success: boolean; data: UserDto }>("/users/me"),
  updateProfile: (data: UpdateProfileRequest) => api<{ success: boolean; data: UserDto }>("/users/me", { method: "PATCH", body: JSON.stringify(data) }),
  getById: (id: string) => api<{ success: boolean; data: UserDto }>(`/users/${id}`),
  search: (q: string) => api<{ success: boolean; data: UserDto[] }>(`/users/search?q=${encodeURIComponent(q)}`),
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
  createPrivate: (data: CreatePrivateConversationRequest) => api<{ success: boolean; data: ConversationDto }>("/conversations/private", { method: "POST", body: JSON.stringify(data) }),
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
  list: (conversationId: string, page = 0, size = 30) => api<{ success: boolean; data: MessageDto[] }>(`/conversations/${conversationId}/messages?page=${page}&size=${size}`),
  send: (conversationId: string, data: SendMessageRequest) => api<{ success: boolean; data: MessageDto }>(`/conversations/${conversationId}/messages`, { method: "POST", body: JSON.stringify(data) }),
  edit: (messageId: string, data: EditMessageRequest) => api<{ success: boolean; data: MessageDto }>(`/messages/${messageId}`, { method: "PATCH", body: JSON.stringify(data) }),
  delete: (messageId: string) => api<{ success: boolean; data: null }>(`/messages/${messageId}`, { method: "DELETE" }),
  addReaction: (messageId: string, data: ReactionRequest) => api<{ success: boolean; data: null }>(`/messages/${messageId}/reaction`, { method: "POST", body: JSON.stringify(data) }),
  removeReaction: (messageId: string, reaction: string) => api<{ success: boolean; data: null }>(`/messages/${messageId}/reaction?reaction=${encodeURIComponent(reaction)}`, { method: "DELETE" }),
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
  create: (data: CreateGroupRequest) => api<{ success: boolean; data: GroupDto }>("/groups", { method: "POST", body: JSON.stringify(data) }),
  get: (id: string) => api<{ success: boolean; data: GroupDto }>(`/groups/${id}`),
  update: (id: string, data: UpdateGroupRequest) => api<{ success: boolean; data: GroupDto }>(`/groups/${id}`, { method: "PATCH", body: JSON.stringify(data) }),
  delete: (id: string) => api<{ success: boolean; data: null }>(`/groups/${id}`, { method: "DELETE" }),
  getMembers: (id: string) => api<{ success: boolean; data: GroupMemberInfo[] }>(`/groups/${id}/members`),
  addMember: (id: string, data: AddMemberRequest) => api<{ success: boolean; data: null }>(`/groups/${id}/members`, { method: "POST", body: JSON.stringify(data) }),
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
  create: (data: CreateChannelRequest) => api<{ success: boolean; data: ChannelDto }>("/channels", { method: "POST", body: JSON.stringify(data) }),
  get: (id: string) => api<{ success: boolean; data: ChannelDto }>(`/channels/${id}`),
  getByUsername: (username: string) => api<{ success: boolean; data: ChannelDto }>(`/channels/username/${username}`),
  getPublic: () => api<{ success: boolean; data: ChannelDto[] }>("/channels/public"),
  update: (id: string, data: UpdateChannelRequest) => api<{ success: boolean; data: ChannelDto }>(`/channels/${id}`, { method: "PATCH", body: JSON.stringify(data) }),
  delete: (id: string) => api<{ success: boolean; data: null }>(`/channels/${id}`, { method: "DELETE" }),
  getSubscribers: (id: string) => api<{ success: boolean; data: ChannelSubscriberInfo[] }>(`/channels/${id}/subscribers`),
  subscribe: (id: string) => api<{ success: boolean; data: null }>(`/channels/${id}/subscribe`, { method: "POST" }),
  unsubscribe: (id: string) => api<{ success: boolean; data: null }>(`/channels/${id}/subscribe`, { method: "DELETE" }),
};

// Files
export type FileDto = { id: string; ownerId: string; storageProvider: string; storageKey: string; originalName: string; mimeType: string; sizeBytes: number; checksum?: string; status: string; createdAt: string };

export type UploadUrlResponse = { fileId: string; uploadUrl: string; storageKey: string };

export const fileApi = {
  requestUploadUrl: (filename: string, mimeType: string, sizeBytes: number) => api<{ success: boolean; data: UploadUrlResponse }>("/files/upload-url", { method: "POST", body: JSON.stringify({ filename, mimeType, sizeBytes }) }),
  completeUpload: (fileId: string, checksum: string) => api<{ success: boolean; data: FileDto }>(`/files/${fileId}/complete`, { method: "POST", body: JSON.stringify({ checksum }) }),
  get: (fileId: string) => api<{ success: boolean; data: FileDto }>(`/files/${fileId}`),
  delete: (fileId: string) => api<{ success: boolean; data: null }>(`/files/${fileId}`, { method: "DELETE" }),
};

// Notifications
export type NotificationDto = { id: string; userId: string; type: string; title: string; body: string; data?: Record<string, unknown>; readAt?: string; createdAt: string };

export const notificationApi = {
  list: () => api<{ success: boolean; data: NotificationDto[] }>("/notifications"),
  markRead: (id: string) => api<{ success: boolean; data: null }>(`/notifications/${id}/read`, { method: "PATCH" }),
};
