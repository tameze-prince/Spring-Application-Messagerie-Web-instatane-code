import { useEffect, useRef, useCallback, useState } from "react";
import { ws } from "@/lib/ws/client";
import { useAuth } from "@/lib/context/AuthContext";

type MessageListener = (data: unknown) => void;
type TypingListener = (data: { conversationId: string; userId: string; username: string }) => void;

export function useWebSocket() {
  const { accessToken, isAuthenticated } = useAuth();
  const [isConnected, setIsConnected] = useState(false);

  // Initialize WebSocket connection
  useEffect(() => {
    if (!isAuthenticated || !accessToken) {
      ws.disconnect();
      setIsConnected(false);
      return;
    }

    ws.connect("ws://localhost:8080/ws", accessToken);

    const checkConnection = setInterval(() => {
      setIsConnected(ws.isConnected);
    }, 1000);

    // Listen for connection status via any message
    const offAll = ws.on("*", () => {
      setIsConnected(true);
    });

    return () => {
      clearInterval(checkConnection);
      offAll();
    };
  }, [isAuthenticated, accessToken]);

  const onMessageCreated = useCallback((listener: MessageListener) => {
    ws.on("message.created", listener);
    return () => {
      ws.off("message.created", listener);
    };
  }, []);

  const onTypingStarted = useCallback((listener: TypingListener) => {
    ws.on("typing.started", listener as MessageListener);
    return () => {
      ws.off("typing.started", listener as MessageListener);
    };
  }, []);

  const onTypingStopped = useCallback((listener: TypingListener) => {
    ws.on("typing.stopped", listener as MessageListener);
    return () => {
      ws.off("typing.stopped", listener as MessageListener);
    };
  }, []);

  const sendMessage = useCallback((conversationId: string, content: string, replyToId?: string) => {
    ws.send({
      clientMessageId: crypto.randomUUID(),
      conversationId,
      content,
      replyToId
    });
  }, []);

  const sendTypingStart = useCallback((conversationId: string) => {
    ws.typingStart(conversationId, "", "");
  }, []);

  const sendTypingStop = useCallback((conversationId: string) => {
    ws.typingStop(conversationId, "", "");
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
