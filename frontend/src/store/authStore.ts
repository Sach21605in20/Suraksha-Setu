import { create } from 'zustand'
import type { AuthState, User } from '@/types/auth'

// Memory-only store — NO sessionStorage / localStorage.
// On page reload, App.tsx calls POST /api/v1/auth/refresh.
// The browser sends the HttpOnly 'refreshToken' cookie automatically,
// restoring the access token without user interaction.
export const useAuthStore = create<AuthState>()((set) => ({
    user: null,
    accessToken: null,
    isAuthenticated: false,
    isInitializing: true, // true until the bootstrap refresh completes or fails

    setAuth: (user: User, token: string) =>
        set({ user, accessToken: token, isAuthenticated: true, isInitializing: false }),

    clearAuth: () =>
        set({ user: null, accessToken: null, isAuthenticated: false, isInitializing: false }),
}))
