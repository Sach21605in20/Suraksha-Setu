import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router-dom'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route
            path="/"
            element={
              <div className="min-h-screen bg-neutral-50 flex items-center justify-center">
                <div className="card max-w-md w-full text-center animate-fade-in">
                  <div className="mb-4">
                    <span className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-primary-100">
                      <svg
                        className="w-8 h-8 text-primary-600"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                        />
                      </svg>
                    </span>
                  </div>
                  <h1 className="text-2xl font-bold text-neutral-900 mb-2">
                    OrthoWatch
                  </h1>
                  <p className="text-neutral-500 text-sm mb-6">
                    Post-Discharge Monitoring System
                  </p>
                  <div className="flex gap-2 justify-center">
                    <span className="badge-risk-low">Frontend ✓</span>
                    <span className="badge-risk-medium">Backend Pending</span>
                  </div>
                </div>
              </div>
            }
          />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

export default App
