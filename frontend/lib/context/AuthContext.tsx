"use client";

import { createContext, useContext, useState, useEffect, useCallback, ReactNode } from "react";
import { api, type AuthResponse, type UserDto, type ApiError } from "../api/client";
import { ws } from "../ws/client";

interface AuthContextType {
  user: UserDto | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (data: { username: string; email: string; password: string; firstName?: string; lastName?: string }) => Promise<void>;
  logout: () => Promise<void>;
  refreshAuth: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const refreshAuth = useCallback(async () => {
    try {
      const storedToken = localStorage.getItem("accessToken");
      const storedRefresh = localStorage.getItem("refreshToken");
      const storedUser = localStorage.getItem("user");
      
      if (storedToken && storedUser) {
        setAccessToken(storedToken);
        setUser(JSON.parse(storedUser));
      } else if (storedRefresh) {
        // Try to refresh token
        const response = await api<{ success: boolean; data: AuthResponse }>("/auth/refresh", {
          method: "POST",
          body: JSON.stringify({ refreshToken: storedRefresh })
        });
        localStorage.setItem("accessToken", response.data.accessToken);
        localStorage.setItem("refreshToken", response.data.refreshToken);
        localStorage.setItem("user", JSON.stringify(response.data.user));
        setAccessToken(response.data.accessToken);
        setUser(response.data.user);
      }
    } catch (error) {
      console.error("Auth refresh failed:", error);
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("user");
      setUser(null);
      setAccessToken(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshAuth();
  }, [refreshAuth]);

  const login = async (email: string, password: string) => {
    const response = await api<{ success: boolean; data: AuthResponse }>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ login: email, password })
    });
    
    localStorage.setItem("accessToken", response.data.accessToken);
    localStorage.setItem("refreshToken", response.data.refreshToken);
    localStorage.setItem("user", JSON.stringify(response.data.user));
    
    setAccessToken(response.data.accessToken);
    setUser(response.data.user);
    
    // Connect WebSocket
    ws.connect("ws://localhost:8080/ws", response.data.accessToken);
  };

  const register = async (data: { username: string; email: string; password: string; firstName?: string; lastName?: string }) => {
    const response = await api<{ success: boolean; data: AuthResponse }>("/auth/register", {
      method: "POST",
      body: JSON.stringify(data)
    });
    
    localStorage.setItem("accessToken", response.data.accessToken);
    localStorage.setItem("refreshToken", response.data.refreshToken);
    localStorage.setItem("user", JSON.stringify(response.data.user));
    
    setAccessToken(response.data.accessToken);
    setUser(response.data.user);
    
    ws.connect("ws://localhost:8080/ws", response.data.accessToken);
  };

  const logout = async () => {
    const refreshToken = localStorage.getItem("refreshToken");
    if (refreshToken) {
      try {
        await api("/auth/logout", {
          method: "POST",
          body: JSON.stringify({ refreshToken })
        });
      } catch (error) {
        console.error("Logout API error:", error);
      }
    }
    
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
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
