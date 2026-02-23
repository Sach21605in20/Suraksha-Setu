// ─── Auth Types ─────────────────────────────────────────────────────────────

export type UserRole = 'ADMIN' | 'SURGEON' | 'NURSE'

export interface User {
    id: string
    email: string
    role: UserRole
    fullName: string
}

export interface LoginRequest {
    email: string
    password: string
}

export interface LoginResponse {
    accessToken: string
    refreshToken?: string // returned by backend but not used client-side (stored in HttpOnly cookie)
    user: User
}

export interface AuthState {
    user: User | null
    accessToken: string | null
    isAuthenticated: boolean
    isInitializing: boolean
    setAuth: (user: User, token: string) => void
    clearAuth: () => void
}
