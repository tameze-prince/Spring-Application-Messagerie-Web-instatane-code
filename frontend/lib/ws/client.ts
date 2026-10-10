import type { SendMessageInput } from "../api/client";

export type Listener = (payload: unknown) => void;

export interface WsEventResponse<T> {
  event: string;
  requestId?: string;
  data: T;
}

export class WavelengthSocket {
  private socket?: WebSocket;
  private listeners = new Map<string, Set<Listener>>();
  private reconnectAttempt = 0;
  private accessToken: string | null = null;
  private shouldReconnect = false;
  private reconnectTimer?: number;

  connect(url: string, token?: string) {
    this.shouldReconnect = true;
    this.accessToken = token ?? (typeof window !== "undefined" ? localStorage.getItem("accessToken") : null);

    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return;
    }

    this.clearReconnectTimer();
    const wsUrl = this.accessToken ? `${url}?token=${encodeURIComponent(this.accessToken)}` : url;

    this.socket = new WebSocket(wsUrl);

    this.socket.onmessage = event => {
      try {
        const message = JSON.parse(event.data) as WsEventResponse<unknown>;
        this.listeners.get(message.event)?.forEach(listener => listener(message.data));
        this.listeners.get("*")?.forEach(listener => listener(message));
      } catch (error) {
        console.error("WS parse error:", error);
      }
    };

    this.socket.onclose = () => {
      this.socket = undefined;
      if (!this.shouldReconnect || typeof window === "undefined") return;

      const delay = Math.min(1000 * 2 ** this.reconnectAttempt++, 15000);
      this.reconnectTimer = window.setTimeout(() => {
        if (this.shouldReconnect) {
          this.connect(url, this.accessToken ?? undefined);
        }
      }, delay);
    };

    this.socket.onopen = () => {
      this.reconnectAttempt = 0;
    };

    this.socket.onerror = error => {
      console.error("WS error:", error);
    };
  }

  on(type: string, listener: Listener) {
    const listeners = this.listeners.get(type) ?? new Set<Listener>();
    listeners.add(listener);
    this.listeners.set(type, listeners);
    return () => listeners.delete(listener);
  }

  off(type: string, listener: Listener) {
    this.listeners.get(type)?.delete(listener);
  }

  send(message: SendMessageInput) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      throw new Error("WebSocket is not connected");
    }
    this.socket.send(JSON.stringify({ type: "message.send", payload: message }));
  }

  typingStart(conversationId: string, userId: string, username: string) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;
    this.socket.send(JSON.stringify({
      type: "typing.start",
      payload: { conversationId, userId, username }
    }));
  }

  typingStop(conversationId: string, userId: string, username: string) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;
    this.socket.send(JSON.stringify({
      type: "typing.stop",
      payload: { conversationId, userId, username }
    }));
  }

  disconnect() {
    this.shouldReconnect = false;
    this.clearReconnectTimer();
    this.reconnectAttempt = 0;
    this.socket?.close();
    this.socket = undefined;
    this.accessToken = null;
  }

  get isConnected() {
    return typeof WebSocket !== "undefined" && this.socket?.readyState === WebSocket.OPEN;
  }

  private clearReconnectTimer() {
    if (this.reconnectTimer !== undefined && typeof window !== "undefined") {
      window.clearTimeout(this.reconnectTimer);
      this.reconnectTimer = undefined;
    }
  }
}

export const ws = new WavelengthSocket();
