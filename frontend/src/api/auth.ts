import type { LoginRequest, LoginResponse } from '@/types/auth'
import apiClient from '@/lib/axios'

/**
 * POST /api/v1/auth/login
 * Returns access token in body + sets HttpOnly refreshToken cookie on response.
 */
export const login = (payload: LoginRequest): Promise<LoginResponse> =>
    apiClient.post<LoginResponse>('/v1/auth/login', payload).then((r) => r.data)

/**
 * POST /api/v1/auth/refresh
 * Browser sends HttpOnly 'refreshToken' cookie automatically (withCredentials: true).
 * Returns a fresh access token + user details.
 */
export const refreshToken = (): Promise<LoginResponse> =>
    apiClient.post<LoginResponse>('/v1/auth/refresh').then((r) => r.data)

/**
 * POST /api/v1/auth/logout
 * Server clears the refreshToken cookie (Max-Age: 0).
 */
export const logout = (): Promise<void> =>
    apiClient.post('/v1/auth/logout').then(() => undefined)
