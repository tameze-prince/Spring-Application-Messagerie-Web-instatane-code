"use client";

import { createContext, useContext, useState, useEffect, useCallback, useRef, ReactNode } from "react";
import { ApiError, authApi, type AuthResponse, type UserDto } from "../api/client";
import { ws } from "../ws/client";

interface AuthContextType {
  user: UserDto | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (login: string, password: string) => Promise<void>;
  register: (data: { username: string; email: string; password: string; firstName?: string; lastName?: string }) => Promise<void>;
  logout: () => Promise<void>;
  refreshAuth: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function persistAuth(data: AuthResponse) {
  localStorage.setItem("accessToken", data.accessToken);
  localStorage.setItem("refreshToken", data.refreshToken);
  localStorage.setItem("user", JSON.stringify(data.user));
}

function clearPersistedAuth() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
}

function getTokenExpiryMs(token: string): number | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
    const decoded = JSON.parse(window.atob(padded));
    return typeof decoded.exp === "number" ? decoded.exp * 1000 : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const refreshTimerRef = useRef<number | null>(null);

  const clearRefreshTimer = useCallback(() => {
    if (refreshTimerRef.current !== null) {
      window.clearTimeout(refreshTimerRef.current);
      refreshTimerRef.current = null;
    }
  }, []);

  const persistAndSetAuth = useCallback((data: AuthResponse) => {
    persistAuth(data);
    setAccessToken(data.accessToken);
    setUser(data.user);
  }, []);

  const refreshAuth = useCallback(async () => {
    try {
      const storedRefresh = localStorage.getItem("refreshToken");
      const storedToken = localStorage.getItem("accessToken");
      const storedUser = localStorage.getItem("user");

      if (storedRefresh) {
        const response = await authApi.refresh({ refreshToken: storedRefresh });
        persistAndSetAuth(response.data);
      } else if (storedToken && storedUser) {
        setAccessToken(storedToken);
        setUser(JSON.parse(storedUser));
      } else {
        setAccessToken(null);
        setUser(null);
      }
    } catch (error) {
      const apiError = error as ApiError;
      console.error("Auth refresh failed:", apiError.message);
      clearPersistedAuth();
      setUser(null);
      setAccessToken(null);
    } finally {
      setIsLoading(false);
    }
  }, [persistAndSetAuth]);

  const scheduleRefresh = useCallback((token: string) => {
    clearRefreshTimer();

    const expiresAt = getTokenExpiryMs(token);
    if (!expiresAt) return;

    const delay = Math.max(expiresAt - Date.now() - 60_000, 5_000);
    refreshTimerRef.current = window.setTimeout(async () => {
      try {
        const refreshToken = localStorage.getItem("refreshToken");
        if (!refreshToken) return;
        const response = await authApi.refresh({ refreshToken });
        persistAndSetAuth(response.data);
      } catch {
        clearPersistedAuth();
        setUser(null);
        setAccessToken(null);
      }
    }, delay);
  }, [clearRefreshTimer, persistAndSetAuth]);

  useEffect(() => {
    refreshAuth();
    return clearRefreshTimer;
  }, [refreshAuth, clearRefreshTimer]);

  useEffect(() => {
    if (accessToken) {
      scheduleRefresh(accessToken);
    } else {
      clearRefreshTimer();
    }
  }, [accessToken, scheduleRefresh, clearRefreshTimer]);

  const login = async (loginValue: string, password: string) => {
    const response = await authApi.login({ login: loginValue.trim(), password });
    persistAndSetAuth(response.data);
  };

  const register = async (data: { username: string; email: string; password: string; firstName?: string; lastName?: string }) => {
    const response = await authApi.register({
      ...data,
      username: data.username.trim(),
      email: data.email.trim().toLowerCase()
    });
    persistAndSetAuth(response.data);
  };

  const logout = async () => {
    clearRefreshTimer();

    const refreshToken = localStorage.getItem("refreshToken");
    if (refreshToken) {
      try {
        await authApi.logout(refreshToken);
      } catch (error) {
        console.error("Logout API error:", error);
      }
    }

    clearPersistedAuth();
    setUser(null);
    setAccessToken(null);
    ws.disconnect();
  };

  return (
    <AuthContext.Provider value={{
      user,
      accessToken,
      isAuthenticated: !!user,
      isLoading,
      login,
      register,
      logout,
      refreshAuth
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
