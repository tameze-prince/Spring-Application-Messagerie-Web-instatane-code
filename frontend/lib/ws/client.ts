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

  connect(url: string, token?: string) {
    this.accessToken = token ?? (typeof window !== "undefined" ? localStorage.getItem("accessToken") : null);
    const wsUrl = this.accessToken ? `${url}?token=${encodeURIComponent(this.accessToken)}` : url;
    
    this.socket = new WebSocket(wsUrl);
    
    this.socket.onmessage = event => {
      try {
        const message = JSON.parse(event.data) as WsEventResponse<unknown>;
        this.listeners.get(message.event)?.forEach(listener => listener(message.data));
        this.listeners.get("*")?.forEach(listener => listener(message));
      } catch (e) {
        console.error("WS parse error:", e);
      }
    };
    
    this.socket.onclose = () => { 
      window.setTimeout(() => this.connect(url, this.accessToken ?? undefined), Math.min(1000 * 2 ** this.reconnectAttempt++, 15000)); 
    };
    
    this.socket.onopen = () => { this.reconnectAttempt = 0; };
    
    this.socket.onerror = (err) => {
      console.error("WS error:", err);
    };
  }

  on(type: string, listener: Listener) { 
    const list = this.listeners.get(type) ?? new Set(); 
    list.add(listener); 
    this.listeners.set(type, list); 
    return () => list.delete(listener); 
  }
  
  off(type: string, listener: Listener) {
    this.listeners.get(type)?.delete(listener);
  }

  send(message: SendMessageInput) { 
    this.socket?.send(JSON.stringify({ type: "message.send", payload: message })); 
  }

  typingStart(conversationId: string, userId: string, username: string) {
    this.socket?.send(JSON.stringify({ 
      type: "typing.start", 
      payload: { conversationId, userId, username } 
    }));
  }

  typingStop(conversationId: string, userId: string, username: string) {
    this.socket?.send(JSON.stringify({ 
      type: "typing.stop", 
      payload: { conversationId, userId, username } 
    }));
  }

  disconnect() {
    this.socket?.close();
    this.socket = undefined;
  }

  get isConnected() {
    return this.socket?.readyState === WebSocket.OPEN;
  }
}

// Singleton instance
export const ws = new WavelengthSocket();
