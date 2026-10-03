import { expect, test } from "@playwright/test";

const apiUrl =
  process.env.NEXT_PUBLIC_API_URL ?? "http://127.0.0.1:8080/api/v1";
const wsUrl =
  process.env.NEXT_PUBLIC_WS_URL ?? "ws://127.0.0.1:8080/ws";

type AuthResponse = {
  success: boolean;
  data: {
    accessToken: string;
    refreshToken: string;
    user: {
      id: string;
      username: string;
    };
  };
};

type ConversationResponse = {
  success: boolean;
  data: {
    id: string;
  };
};

function stompFrame(
  command: string,
  headers: Record<string, string>,
  body = "",
) {
  const headerLines = Object.entries(headers)
    .map(([key, value]) => `${key}:${value}`)
    .join("\n");

  return `${command}\n${headerLines}\n\n${body}\0`;
}

test("two authenticated clients exchange a message over STOMP", async ({
  page,
  request,
}) => {
  const unique = Date.now();

  async function register(username: string): Promise<AuthResponse["data"]> {
    const response = await request.post(apiUrl + "/auth/register", {
      data: {
        username,
        email: username + "@example.com",
        password: "password123",
        firstName: "E2E",
        lastName: "Tester",
      },
    });

    expect(response.ok()).toBeTruthy();
    const payload = (await response.json()) as AuthResponse;
    expect(payload.success).toBeTruthy();
    return payload.data;
  }

  const alice = await register("e2ealice" + unique);
  const bob = await register("e2ebob" + unique);

  const conversationResponse = await request.post(
    apiUrl + "/conversations/private",
    {
      headers: {
        Authorization: "Bearer " + alice.accessToken,
      },
      data: {
        targetUserId: bob.user.id,
      },
    },
  );

  expect(conversationResponse.ok()).toBeTruthy();
  const conversation = (
    (await conversationResponse.json()) as ConversationResponse
  ).data;

  await page.goto("/login");

  await page.evaluate(
    async ({ url, aliceToken, bobToken, conversationId }) => {
      const connect = (token: string) =>
        new Promise<void>((resolve, reject) => {
          const socket = new WebSocket(url);
          let buffer = "";

          const handle = (raw: string) => {
            buffer += raw;
            const end = buffer.indexOf("\0");
            if (end < 0) return;

            const frame = buffer.slice(0, end);
            buffer = buffer.slice(end + 1);

            if (frame.startsWith("CONNECTED\n")) {
              resolve();
            } else if (frame.startsWith("ERROR\n")) {
              reject(new Error(frame));
            }
          };

          socket.onopen = () => {
            socket.send(
              "CONNECT\n" +
                "accept-version:1.2\n" +
                "Authorization:Bearer " +
                token +
                "\n" +
                "heart-beat:0,0\n\n\0",
            );
          };

          socket.onmessage = event => handle(String(event.data));
          socket.onerror = () => reject(new Error("WebSocket connection failed"));

          if (!("testSockets" in window)) {
            (window as unknown as { testSockets?: WebSocket[] }).testSockets = [];
          }

          (
            window as unknown as { testSockets: WebSocket[] }
          ).testSockets.push(socket);
        });

      await Promise.all([connect(aliceToken), connect(bobToken)]);

      const sockets = (
        window as unknown as { testSockets: WebSocket[] }
      ).testSockets;

      const bobSocket = sockets[1];

      await new Promise<void>((resolve, reject) => {
        const subscriptionId = "e2e-sub-" + conversationId;
        const timeout = window.setTimeout(
          () => reject(new Error("Timed out waiting for CONNECT/SUBSCRIBE")),
          5000,
        );

        const original = bobSocket.onmessage;
        bobSocket.onmessage = event => {
          const raw = String(event.data);
          if (raw.includes("\n\n") && raw.startsWith("ERROR")) {
            window.clearTimeout(timeout);
            reject(new Error(raw));
            return;
          }

          original?.call(bobSocket, event);
        };

        bobSocket.send(
          stompFrame("SUBSCRIBE", {
            id: subscriptionId,
            destination: "/topic/conversations/" + conversationId,
            ack: "auto",
          }),
        );

        window.setTimeout(() => {
          window.clearTimeout(timeout);
          resolve();
        }, 300);
      });

      (window as unknown as {
        e2eMessagePromise?: Promise<unknown>;
      }).e2eMessagePromise = new Promise((resolve, reject) => {
        const bobSocket = (
          window as unknown as { testSockets: WebSocket[] }
        ).testSockets[1];
        const timeout = window.setTimeout(
          () => reject(new Error("Timed out waiting for message.created")),
          8000,
        );

        bobSocket.onmessage = event => {
          const raw = String(event.data);
          if (!raw.startsWith("MESSAGE\n")) return;

          const bodyStart = raw.indexOf("\n\n");
          if (bodyStart < 0) return;

          const body = raw
            .slice(bodyStart + 2)
            .replace(/\0$/, "")
            .trim();

          try {
            const payload = JSON.parse(body) as {
              event?: string;
              data?: { body?: string };
            };

            if (payload.event === "message.created") {
              window.clearTimeout(timeout);
              resolve(payload.data);
            }
          } catch {
            // Ignore unrelated frames.
          }
        };
      });

      const message = {
        requestId: "e2e-" + unique,
        conversationId,
        clientMessageId: "client-" + unique,
        type: "TEXT",
        body: "Hello from STOMP E2E",
      };

      sockets[0].send(
        stompFrame("SEND", {
          destination: "/app/message.send",
          "content-type": "application/json",
        }, JSON.stringify(message)),
      );
    },
    {
      url: wsUrl,
      aliceToken: alice.accessToken,
      bobToken: bob.accessToken,
      conversationId: conversation.id,
    },
  );

  const received = await page.evaluate(async () => {
    return await (
      window as unknown as {
        e2eMessagePromise: Promise<{ body?: string }>;
      }
    ).e2eMessagePromise;
  });

  expect(received.body).toBe("Hello from STOMP E2E");
});
