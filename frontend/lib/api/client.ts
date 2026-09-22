/** Typed REST client boundary. Set NEXT_PUBLIC_API_URL to your Spring API origin. */
const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) { super(message); }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: { "Content-Type": "application/json", ...init.headers },
    credentials: "include",
  });
  if (!response.ok) throw new ApiError(response.status, (await response.text()) || "Request failed");
  return response.status === 204 ? (undefined as T) : response.json() as Promise<T>;
}

export type SendMessageInput = { clientMessageId: string; conversationId: string; content: string; replyToId?: string };
export type MessageReceipt = "SENDING" | "SENT" | "DELIVERED" | "READ" | "FAILED";
