import { describe, expect, it } from "vitest";
import { ApiError, authApi, channelApi, fileApi, groupApi, messageApi, userApi } from "../client";

describe("frontend API input validation", () => {
  it("rejects invalid registration fields before the HTTP request", () => {
    expect(() => authApi.register({
      username: "ab",
      email: "alice@example.com",
      password: "password123",
    })).toThrow(ApiError);

    expect(() => authApi.register({
      username: "alice",
      email: "not-an-email",
      password: "password123",
    })).toThrow(ApiError);

    expect(() => authApi.register({
      username: "alice",
      email: "alice@example.com",
      password: "short",
    })).toThrow(ApiError);
  });

  it("rejects invalid login lengths before the HTTP request", () => {
    expect(() => authApi.login({ login: "alice", password: "short" })).toThrow(ApiError);
    expect(() => authApi.login({ login: "x".repeat(256), password: "password123" })).toThrow(ApiError);
  });

  it("rejects empty and overlong messages", () => {
    expect(() => messageApi.send("conversation-id", { body: "   " })).toThrow(ApiError);
    expect(() => messageApi.send("conversation-id", { body: "x".repeat(4001) })).toThrow(ApiError);
    expect(() => messageApi.edit("message-id", { body: "" })).toThrow(ApiError);
  });

  it("rejects invalid group and channel payloads", () => {
    expect(() => groupApi.create({ name: " ", maxMembers: 1 })).toThrow(ApiError);
    expect(() => groupApi.create({ name: "Valid name", maxMembers: 501 })).toThrow(ApiError);
    expect(() => channelApi.create({ name: "News", username: "bad name" })).toThrow(ApiError);
    expect(() => channelApi.create({ name: "News", username: "news", visibility: "WORLD" })).toThrow(ApiError);
  });

  it("rejects unsafe file metadata and file sizes", () => {
    expect(() => fileApi.requestUploadUrl("../secret.txt", "text/plain", 10)).toThrow(ApiError);
    expect(() => fileApi.requestUploadUrl("photo.png", "image/png", 0)).toThrow(ApiError);
    expect(() => fileApi.requestUploadUrl("photo.png", "image/png", 50 * 1024 * 1024 + 1)).toThrow(ApiError);
    expect(() => fileApi.completeUpload("not-a-uuid", "checksum")).toThrow(ApiError);
  });

  it("limits user search before creating the request", () => {
    expect(() => userApi.search("x".repeat(101))).toThrow(ApiError);
  });
});
