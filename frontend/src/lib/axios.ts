import axios from 'axios'
import { useAuthStore } from '@/store/authStore'

const apiClient = axios.create({
    baseURL: '/api',
    withCredentials: true, // sends HttpOnly 'refreshToken' cookie on every request
    headers: {
        'Content-Type': 'application/json',
    },
})

// ─── Request Interceptor ─────────────────────────────────────────────────────
// Attaches the in-memory access token to every request.
apiClient.interceptors.request.use((config) => {
    const token = useAuthStore.getState().accessToken
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

// ─── Response Interceptor ────────────────────────────────────────────────────
// On 401: clear auth state and redirect to /login.
// Normalises all error payloads to { message: string }.
apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            useAuthStore.getState().clearAuth()
            // Avoid redirect loop if already on /login
            if (window.location.pathname !== '/login') {
                window.location.replace('/login')
            }
        }

        const message: string =
            error.response?.data?.message ??
            error.response?.data?.error ??
            error.message ??
            'An unexpected error occurred'

        return Promise.reject(new Error(message))
    },
)

export default apiClient
