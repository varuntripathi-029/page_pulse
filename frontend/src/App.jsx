import { useState } from 'react'
import AuditForm from './components/AuditForm.jsx'
import AuditResult from './components/AuditResult.jsx'
import ErrorCard from './components/ErrorCard.jsx'
import Footer from './components/Footer.jsx'
import { runAudit } from './api/auditApi.js'

function App() {
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [isLoading, setIsLoading] = useState(false)

  async function handleAudit(url) {
    setIsLoading(true)
    setError(null)
    setResult(null)

    try {
      const data = await runAudit(url)
      setResult(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-white text-black flex flex-col">
      <main className="flex-1 w-full max-w-xl mx-auto px-6 py-12">
        <h1 className="text-3xl font-semibold mb-1">Page Pulse</h1>
        <p className="text-gray-600 mb-6">Enter a URL to audit its webpage.</p>

        <AuditForm onSubmit={handleAudit} isLoading={isLoading} />

        {isLoading && <p className="text-gray-600 mt-6">Loading…</p>}
        {error && <ErrorCard message={error} />}
        {result && <AuditResult result={result} />}
      </main>
      <Footer />
    </div>
  )
}

export default App
