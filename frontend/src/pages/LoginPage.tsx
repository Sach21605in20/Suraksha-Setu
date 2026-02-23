import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Eye, EyeOff, ShieldCheck } from 'lucide-react'
import { login } from '@/api/auth'
import { useAuthStore } from '@/store/authStore'
import { LoadingSpinner } from '@/components/LoadingSpinner'

// ─── Validation Schema ────────────────────────────────────────────────────────
const loginSchema = z.object({
    email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
    password: z.string().min(6, 'Password must be at least 6 characters'),
})

type LoginFormValues = z.infer<typeof loginSchema>

// ─── Component ────────────────────────────────────────────────────────────────
export function LoginPage() {
    const navigate = useNavigate()
    const { setAuth } = useAuthStore()
    const [apiError, setApiError] = useState<string | null>(null)
    const [showPassword, setShowPassword] = useState(false)

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<LoginFormValues>({
        resolver: zodResolver(loginSchema),
    })

    const onSubmit = async (data: LoginFormValues) => {
        setApiError(null)
        try {
            const response = await login(data)
            setAuth(response.user, response.accessToken)
            navigate('/dashboard', { replace: true })
        } catch (err) {
            setApiError(err instanceof Error ? err.message : 'Login failed. Please try again.')
        }
    }

    return (
        <div className="min-h-screen bg-neutral-50 flex items-center justify-center px-4">
            <div className="w-full max-w-md animate-fade-in">
                {/* ── Header ── */}
                <div className="text-center mb-8">
                    <div className="inline-flex items-center justify-center w-14 h-14 rounded-full bg-primary-100 mb-4">
                        <ShieldCheck className="w-7 h-7 text-primary-600" aria-hidden="true" />
                    </div>
                    <h1 className="text-2xl font-bold text-neutral-900">OrthoWatch</h1>
                    <p className="text-sm text-neutral-500 mt-1">Post-Discharge Monitoring System</p>
                </div>

                {/* ── Login Card ── */}
                <div className="card">
                    <h2 className="text-lg font-semibold text-neutral-800 mb-6">Sign in to your account</h2>

                    {/* API-level error */}
                    {apiError && (
                        <div
                            role="alert"
                            className="mb-4 rounded-md bg-danger-50 border border-danger-200 px-4 py-3 text-sm text-danger-700"
                        >
                            {apiError}
                        </div>
                    )}

                    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
                        {/* Email */}
                        <div>
                            <label htmlFor="email" className="label">
                                Email address
                            </label>
                            <input
                                id="email"
                                type="email"
                                autoComplete="email"
                                autoFocus
                                disabled={isSubmitting}
                                className={`input ${errors.email ? 'input-error' : ''}`}
                                placeholder="clinician@hospital.com"
                                {...register('email')}
                            />
                            {errors.email && (
                                <p className="error-text" role="alert">
                                    {errors.email.message}
                                </p>
                            )}
                        </div>

                        {/* Password */}
                        <div>
                            <label htmlFor="password" className="label">
                                Password
                            </label>
                            <div className="relative">
                                <input
                                    id="password"
                                    type={showPassword ? 'text' : 'password'}
                                    autoComplete="current-password"
                                    disabled={isSubmitting}
                                    className={`input pr-10 ${errors.password ? 'input-error' : ''}`}
                                    placeholder="••••••••"
                                    {...register('password')}
                                />
                                <button
                                    type="button"
                                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                                    onClick={() => setShowPassword((v) => !v)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-600"
                                    tabIndex={-1}
                                >
                                    {showPassword ? (
                                        <EyeOff className="h-4 w-4" aria-hidden="true" />
                                    ) : (
                                        <Eye className="h-4 w-4" aria-hidden="true" />
                                    )}
                                </button>
                            </div>
                            {errors.password && (
                                <p className="error-text" role="alert">
                                    {errors.password.message}
                                </p>
                            )}
                        </div>

                        {/* Submit */}
                        <button
                            id="login-submit"
                            type="submit"
                            disabled={isSubmitting}
                            className="btn-primary btn-lg w-full mt-2"
                        >
                            {isSubmitting ? (
                                <>
                                    <LoadingSpinner size="sm" />
                                    Signing in…
                                </>
                            ) : (
                                'Sign In'
                            )}
                        </button>
                    </form>
                </div>

                <p className="text-center text-xs text-neutral-400 mt-6">
                    OrthoWatch v0.1 · For authorised clinical staff only
                </p>
            </div>
        </div>
    )
}
