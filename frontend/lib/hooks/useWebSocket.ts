import { useEffect, useCallback, useState } from "react";
import { ws } from "@/lib/ws/client";
import { useAuth } from "@/lib/context/AuthContext";

type MessageListener = (data: unknown) => void;
type TypingListener = (data: { conversationId: string; userId: string; username: string }) => void;

const wsUrl = process.env.NEXT_PUBLIC_WS_URL ?? "ws://localhost:8080/ws";

export function useWebSocket() {
  const { accessToken, isAuthenticated } = useAuth();
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    if (!isAuthenticated || !accessToken) {
      ws.disconnect();
      setIsConnected(false);
      return;
    }

    ws.connect(wsUrl, accessToken);

    const checkConnection = window.setInterval(() => {
      setIsConnected(ws.isConnected);
    }, 500);

    const offAll = ws.on("*", () => {
      setIsConnected(ws.isConnected);
    });

    return () => {
      window.clearInterval(checkConnection);
      offAll();
    };
  }, [accessToken, isAuthenticated]);

  const onMessageCreated = useCallback((listener: MessageListener) => {
    return ws.on("message.created", listener);
  }, []);

  const onTypingStarted = useCallback((listener: TypingListener) => {
    return ws.on("typing.started", listener as MessageListener);
  }, []);

  const onTypingStopped = useCallback((listener: TypingListener) => {
    return ws.on("typing.stopped", listener as MessageListener);
  }, []);

  const sendMessage = useCallback((conversationId: string, content: string, replyToId?: string) => {
    ws.send({
      clientMessageId: crypto.randomUUID(),
      conversationId,
      content,
      replyToId
    });
  }, []);

  const sendTypingStart = useCallback((conversationId: string, userId = "", username = "") => {
    ws.typingStart(conversationId, userId, username);
  }, []);

  const sendTypingStop = useCallback((conversationId: string, userId = "", username = "") => {
    ws.typingStop(conversationId, userId, username);
  }, []);

  return {
    isConnected,
    onMessageCreated,
    onTypingStarted,
    onTypingStopped,
    sendMessage,
    sendTypingStart,
    sendTypingStop
  };
}
