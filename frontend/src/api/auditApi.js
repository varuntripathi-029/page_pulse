// In dev mode (`npm run dev`, served by the Vite dev server on :5173), hit the
// separately-running backend at VITE_API_BASE_URL (default http://localhost:8080).
// In a production build (`npm run build`), always use a relative path so the app
// works same-origin when served by the same Spring Boot process. This is
// intentionally NOT read from VITE_API_BASE_URL in production: Vite loads plain
// `.env` files in every mode (dev AND build), so if frontend/.env is left with
// VITE_API_BASE_URL=http://localhost:8080, a naive fallback could leak that value
// into the production bundle. Gating on import.meta.env.DEV means the production
// branch never reads VITE_API_BASE_URL at all.
const API_BASE_URL = import.meta.env.DEV
  ? (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080')
  : ''

export async function runAudit(url) {
  const response = await fetch(`${API_BASE_URL}/api/audit`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ url }),
  })

  const data = await response.json()

  if (!response.ok) {
    throw new Error(data.error || 'Something went wrong')
  }

  return data
}
