import { useNavigate } from 'react-router-dom'
import { LogOut, User as UserIcon, Stethoscope } from 'lucide-react'
import { logout } from '@/api/auth'
import { useAuthStore } from '@/store/authStore'
import type { UserRole } from '@/types/auth'

const roleLabels: Record<UserRole, string> = {
    ADMIN: 'System Administrator',
    SURGEON: 'Surgeon',
    NURSE: 'Nurse',
}

const roleGreetings: Record<UserRole, string> = {
    ADMIN: 'Manage the system, users, and hospital settings.',
    SURGEON: "Review your patients' recovery progress and risk alerts.",
    NURSE: 'Check outstanding alerts and respond to patient updates.',
}

export function DashboardPage() {
    const navigate = useNavigate()
    const { user, clearAuth } = useAuthStore()

    const handleLogout = async () => {
        try {
            await logout()
        } finally {
            // Always clear local auth state, even if logout API fails
            clearAuth()
            navigate('/login', { replace: true })
        }
    }

    if (!user) return null

    return (
        <div className="min-h-screen bg-neutral-50">
            {/* ── Top Nav ── */}
            <header className="bg-white border-b border-neutral-200 px-6 py-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center">
                        <Stethoscope className="w-4 h-4 text-primary-600" aria-hidden="true" />
                    </div>
                    <span className="font-semibold text-neutral-900">OrthoWatch</span>
                </div>

                <div className="flex items-center gap-4">
                    <div className="flex items-center gap-2 text-sm text-neutral-600">
                        <UserIcon className="w-4 h-4" aria-hidden="true" />
                        <span>{user.email}</span>
                        <span className="badge-risk-low">{user.role}</span>
                    </div>
                    <button
                        id="logout-btn"
                        onClick={handleLogout}
                        className="btn-ghost btn-sm flex items-center gap-1.5"
                    >
                        <LogOut className="w-4 h-4" aria-hidden="true" />
                        Logout
                    </button>
                </div>
            </header>

            {/* ── Main Content ── */}
            <main className="max-w-4xl mx-auto px-6 py-10">
                <div className="card animate-fade-in">
                    <h1 className="text-2xl font-bold text-neutral-900 mb-1">
                        Welcome, {user.fullName || user.email}
                    </h1>
                    <p className="text-sm text-neutral-500 mb-2">{roleLabels[user.role]}</p>
                    <p className="text-sm text-neutral-600 mb-6">{roleGreetings[user.role]}</p>

                    <div className="rounded-md bg-warning-50 border border-warning-200 px-4 py-3 text-sm text-warning-700">
                        <strong>Dashboard under construction.</strong> Patient list, risk alerts, and trend
                        charts are coming in Phase 3.
                    </div>
                </div>
            </main>
        </div>
    )
}
