import type { SendMessageInput } from "../api/client";

export type Listener = (payload: unknown) => void;

type Subscription = {
  destination: string;
  listeners: Set<Listener>;
};

export interface WsEventResponse<T> {
  event: string;
  requestId?: string;
  data: T;
}

type StompFrame = {
  command: string;
  headers: Record<string, string>;
  body: string;
};

export class WavelengthSocket {
  private socket?: WebSocket;
  private listeners = new Map<string, Set<Listener>>();
  private subscriptions = new Map<string, Subscription>();
  private activeSubscriptions = new Set<string>();
  private reconnectAttempt = 0;
  private accessToken: string | null = null;
  private shouldReconnect = false;
  private reconnectTimer?: number;
  private connectUrl = "";
  private connected = false;
  private subscriptionCounter = 0;
  private incomingBuffer = "";

  connect(url: string, token?: string) {
    this.shouldReconnect = true;
    this.accessToken = token ?? (
      typeof window !== "undefined"
        ? localStorage.getItem("accessToken")
        : null
    );
    this.connectUrl = url;

    if (!this.accessToken || this.isSocketOpen()) return;

    this.clearReconnectTimer();
    this.connected = false;
    this.activeSubscriptions.clear();
    this.incomingBuffer = "";
    this.socket = new WebSocket(url);

    this.socket.onopen = () => {
      this.sendFrame("CONNECT", {
        "accept-version": "1.2",
        host: "wavelength",
        Authorization: "Bearer " + this.accessToken,
        "heart-beat": "0,0",
      });
    };

    this.socket.onmessage = event => {
      this.handleIncomingData(String(event.data));
    };

    this.socket.onclose = () => {
      this.socket = undefined;
      this.connected = false;
      this.activeSubscriptions.clear();

      if (!this.shouldReconnect || typeof window === "undefined") return;

      const delay = Math.min(1000 * 2 ** this.reconnectAttempt++, 15000);
      this.reconnectTimer = window.setTimeout(() => {
        if (this.shouldReconnect) {
          this.connect(this.connectUrl, this.accessToken ?? undefined);
        }
      }, delay);
    };

    this.socket.onerror = error => {
      console.error("WS error:", error);
    };
  }

  subscribe(destination: string, listener: Listener) {
    const existing = this.subscriptions.get(destination);

    if (existing) {
      existing.listeners.add(listener);
    } else {
      this.subscriptions.set(destination, {
        destination,
        listeners: new Set([listener]),
      });
    }

    if (this.connected) {
      this.sendSubscribe(destination);
    }

    return () => {
      const subscription = this.subscriptions.get(destination);
      if (!subscription) return;

      subscription.listeners.delete(listener);

      if (subscription.listeners.size > 0) return;

      if (this.connected && this.activeSubscriptions.has(destination)) {
        this.sendFrame("UNSUBSCRIBE", {
          id: this.subscriptionId(destination),
        });
        this.activeSubscriptions.delete(destination);
      }

      this.subscriptions.delete(destination);
    };
  }

  subscribeConversation(conversationId: string, listener: Listener) {
    return this.subscribe(
      "/topic/conversations/" + conversationId,
      listener,
    );
  }

  on(type: string, listener: Listener) {
    const eventListeners = this.listeners.get(type) ?? new Set<Listener>();
    eventListeners.add(listener);
    this.listeners.set(type, eventListeners);

    return () => eventListeners.delete(listener);
  }

  off(type: string, listener: Listener) {
    this.listeners.get(type)?.delete(listener);
  }

  sendMessage(message: SendMessageInput) {
    this.send("/app/message.send", message);
  }

  send(destination: string, payload: unknown) {
    if (!this.connected) {
      throw new Error("WebSocket is not connected");
    }

    this.sendFrame(
      "SEND",
      {
        destination,
        "content-type": "application/json",
      },
      JSON.stringify(payload),
    );
  }

  typingStart(conversationId: string) {
    this.send("/app/typing.start", { conversationId });
  }

  typingStop(conversationId: string) {
    this.send("/app/typing.stop", { conversationId });
  }

  disconnect() {
    this.shouldReconnect = false;
    this.clearReconnectTimer();

    if (this.connected && this.socket?.readyState === WebSocket.OPEN) {
      this.sendFrame("DISCONNECT", { receipt: "disconnect" });
    }

    this.socket?.close();
    this.socket = undefined;
    this.accessToken = null;
    this.connected = false;
    this.activeSubscriptions.clear();
    this.incomingBuffer = "";
    this.reconnectAttempt = 0;
  }

  get isConnected() {
    return this.connected && this.isSocketOpen();
  }

  private handleIncomingData(data: string) {
    this.incomingBuffer += data;

    let boundary = this.incomingBuffer.indexOf("\0");
    while (boundary >= 0) {
      const rawFrame = this.incomingBuffer.slice(0, boundary);
      this.incomingBuffer = this.incomingBuffer.slice(boundary + 1);

      if (rawFrame.trim()) {
        this.handleFrame(this.parseFrame(rawFrame));
      }

      boundary = this.incomingBuffer.indexOf("\0");
    }
  }

  private handleFrame(frame: StompFrame) {
    if (frame.command === "CONNECTED") {
      this.connected = true;
      this.reconnectAttempt = 0;
      this.restoreSubscriptions();
      this.emit("*", frame);
      return;
    }

    if (frame.command === "MESSAGE") {
      const destination = frame.headers.destination;
      if (!destination) return;

      const payload = this.parseBody(frame.body);
      this.subscriptions.get(destination)?.listeners.forEach(listener => {
        listener(payload);
      });

      if (
        payload &&
        typeof payload === "object" &&
        "event" in payload
      ) {
        const eventPayload = payload as WsEventResponse<unknown>;
        this.emit(eventPayload.event, eventPayload.data);
      }

      this.emit("*", payload);
      return;
    }

    if (frame.command === "ERROR") {
      console.error("STOMP error:", frame.body);
      this.socket?.close();
    }
  }

  private restoreSubscriptions() {
    this.activeSubscriptions.clear();

    for (const subscription of this.subscriptions.values()) {
      this.sendSubscribe(subscription.destination);
    }
  }

  private sendSubscribe(destination: string) {
    if (this.activeSubscriptions.has(destination)) return;

    this.sendFrame("SUBSCRIBE", {
      id: this.subscriptionId(destination),
      destination,
      ack: "auto",
    });

    this.activeSubscriptions.add(destination);
  }

  private subscriptionId(destination: string) {
    const stable = destination.replace(/[^a-zA-Z0-9]/g, "-");
    return "sub-" + stable + "-" + this.subscriptionCounter++;
  }

  private sendFrame(
    command: string,
    headers: Record<string, string>,
    body = "",
  ) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;

    const headerBlock = Object.entries(headers)
      .map(([key, value]) => key + ":" + this.escapeHeader(value))
      .join("\n");

    this.socket.send(
      command + "\n" + headerBlock + "\n\n" + body + "\0",
    );
  }

  private parseFrame(raw: string): StompFrame {
    const frame = raw.replace(/^\n+/, "");
    const commandEnd = frame.indexOf("\n");
    const headerEnd = frame.indexOf("\n\n");

    if (commandEnd < 0 || headerEnd < 0) {
      throw new Error("Invalid STOMP frame");
    }

    const command = frame.slice(0, commandEnd).trim();
    const headerLines = frame
      .slice(commandEnd + 1, headerEnd)
      .split("\n")
      .filter(Boolean);

    const headers: Record<string, string> = {};

    for (const line of headerLines) {
      const separator = line.indexOf(":");
      if (separator < 0) continue;

      const key = line.slice(0, separator);
      const value = line.slice(separator + 1);
      headers[key] = this.unescapeHeader(value);
    }

    return {
      command,
      headers,
      body: frame.slice(headerEnd + 2),
    };
  }

  private parseBody(body: string): unknown {
    if (!body) return null;

    try {
      return JSON.parse(body);
    } catch {
      return body;
    }
  }

  private emit(type: string, payload: unknown) {
    this.listeners.get(type)?.forEach(listener => listener(payload));
  }

  private escapeHeader(value: string) {
    return value
      .replace(/\\/g, "\\\\")
      .replace(/\n/g, "\\n")
      .replace(/:/g, "\\c");
  }

  private unescapeHeader(value: string) {
    return value
      .replace(/\\n/g, "\n")
      .replace(/\\c/g, ":")
      .replace(/\\\\/g, "\\");
  }

  private isSocketOpen() {
    return (
      typeof WebSocket !== "undefined" &&
      !!this.socket &&
      this.socket.readyState === WebSocket.OPEN
    );
  }

  private clearReconnectTimer() {
    if (
      this.reconnectTimer !== undefined &&
      typeof window !== "undefined"
    ) {
      window.clearTimeout(this.reconnectTimer);
      this.reconnectTimer = undefined;
    }
  }
}

export const ws = new WavelengthSocket();
