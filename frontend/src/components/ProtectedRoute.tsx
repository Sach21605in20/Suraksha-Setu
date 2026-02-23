import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { LoadingSpinner } from '@/components/LoadingSpinner'

/**
 * Wraps protected routes. Behaviour:
 * - While isInitializing (silent refresh in flight): renders a full-screen spinner
 * - After init, unauthenticated: redirects to /login
 * - After init, authenticated: renders child routes via <Outlet />
 */
export function ProtectedRoute() {
    const { isAuthenticated, isInitializing } = useAuthStore()

    if (isInitializing) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-neutral-50">
                <div className="flex flex-col items-center gap-3">
                    <LoadingSpinner size="lg" />
                    <p className="text-sm text-neutral-500">Loading…</p>
                </div>
            </div>
        )
    }

    return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />
}
