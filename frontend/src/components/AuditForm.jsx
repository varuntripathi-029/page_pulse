import { useState } from 'react'

const EXAMPLE_URL = 'https://stripe.com/in'

function AuditForm({ onSubmit, isLoading }) {
  const [url, setUrl] = useState('')

  function handleSubmit(event) {
    event.preventDefault()
    if (!url.trim()) {
      return
    }
    onSubmit(url.trim())
  }

  function handleTryExample() {
    setUrl(EXAMPLE_URL)
    onSubmit(EXAMPLE_URL)
  }

  return (
    <div className="mb-6">
      <form className="flex gap-2" onSubmit={handleSubmit}>
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
      <button
        type="button"
        onClick={handleTryExample}
        disabled={isLoading}
        className="mt-2 text-sm text-gray-600 underline hover:text-black disabled:text-gray-300 disabled:cursor-not-allowed"
      >
        No URL handy? Try an example: {EXAMPLE_URL}
      </button>
    </div>
  )
}

export default AuditForm
