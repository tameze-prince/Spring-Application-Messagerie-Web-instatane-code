import type { SendMessageInput } from "../api/client";

type Listener = (payload: unknown) => void;
/** Small, transport-agnostic event bus ready for the backend WS protocol. */
export class WavelengthSocket {
  private socket?: WebSocket;
  private listeners = new Map<string, Set<Listener>>();
  private reconnectAttempt = 0;
  connect(url: string) {
    this.socket = new WebSocket(url);
    this.socket.onmessage = event => {
      const message = JSON.parse(event.data) as { type: string; payload: unknown };
      this.listeners.get(message.type)?.forEach(listener => listener(message.payload));
    };
    this.socket.onclose = () => { window.setTimeout(() => this.connect(url), Math.min(1000 * 2 ** this.reconnectAttempt++, 15000)); };
    this.socket.onopen = () => { this.reconnectAttempt = 0; };
  }
  on(type: string, listener: Listener) { const list = this.listeners.get(type) ?? new Set(); list.add(listener); this.listeners.set(type, list); return () => list.delete(listener); }
  send(message: SendMessageInput) { this.socket?.send(JSON.stringify({ type: "message.send", payload: message })); }
}
