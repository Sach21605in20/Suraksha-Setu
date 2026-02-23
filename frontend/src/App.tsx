import { useEffect } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { LoginPage } from '@/pages/LoginPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { useAuthStore } from '@/store/authStore'
import { refreshToken } from '@/api/auth'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
  },
})

// ─── Bootstrap ────────────────────────────────────────────────────────────────
// Separated so it has access to the router context but runs once on mount.
function AuthBootstrap() {
  const { setAuth, clearAuth } = useAuthStore()

  useEffect(() => {
    // Attempt silent refresh using the HttpOnly cookie.
    // withCredentials: true on the Axios instance sends the cookie automatically.
    refreshToken()
      .then((res) => setAuth(res.user, res.accessToken))
      .catch(() => clearAuth()) // cookie absent or expired → stay logged out
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return null
}

// ─── App ──────────────────────────────────────────────────────────────────────
function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthBootstrap />
        <Routes>
          {/* Public */}
          <Route path="/login" element={<LoginPage />} />

          {/* Protected */}
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<DashboardPage />} />
          </Route>

          {/* Catch-all → dashboard (ProtectedRoute will redirect to /login if needed) */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

export default App
