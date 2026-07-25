import { useState } from 'react'

function AuditForm({ onSubmit, isLoading }) {
  const [url, setUrl] = useState('')

  function handleSubmit(event) {
    event.preventDefault()
    if (!url.trim()) {
      return
    }
    onSubmit(url.trim())
  }

  return (
    <form className="flex gap-2 mb-6" onSubmit={handleSubmit}>
      <input
        type="text"
        className="flex-1 px-3 py-2 text-base bg-white text-black border border-gray-300 rounded-md focus:outline-none focus:border-black disabled:bg-gray-100"
        placeholder="https://example.com"
        value={url}
        onChange={(event) => setUrl(event.target.value)}
        disabled={isLoading}
        aria-label="Webpage URL"
      />
      <button
        type="submit"
        className="px-5 py-2 text-base font-medium bg-black text-white rounded-md hover:bg-gray-800 disabled:bg-gray-400 disabled:cursor-not-allowed"
        disabled={isLoading}
      >
        {isLoading ? 'Auditing…' : 'Audit'}
      </button>
    </form>
  )
}

export default AuditForm
