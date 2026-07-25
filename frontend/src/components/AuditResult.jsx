function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`
  return `${(bytes / 1024).toFixed(1)} KB`
}

function Row({ label, value }) {
  return (
    <div className="flex justify-between gap-4 py-2.5 border-b border-gray-200 last:border-b-0">
      <dt className="text-gray-600">{label}</dt>
      <dd className="m-0 font-semibold text-black text-right break-words">{value}</dd>
    </div>
  )
}

function Section({ title, children }) {
  return (
    <div className="mb-6 last:mb-0">
      <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2">
        {title}
      </h3>
      <dl className="m-0 bg-gray-50 border border-gray-200 rounded-lg px-4">{children}</dl>
    </div>
  )
}

function AuditResult({ result }) {
  const { headingCounts, images, seoTags, timing } = result

  return (
    <div className="bg-gray-50 border border-gray-200 rounded-lg p-6">
      <h2 className="text-xl font-semibold text-black mb-4">Audit Report</h2>

      <Section title="Overview">
        <Row label="HTTP Status" value={result.status} />
        <Row label="Response Time" value={`${result.responseTime} ms`} />
        <Row label="Page Size" value={formatBytes(result.pageSizeBytes)} />
        <Row label="Page Title" value={result.title || '(empty)'} />
        <Row label="Meta Description" value={result.metaDescription || '(empty)'} />
        <Row label="Word Count" value={result.wordCount} />
      </Section>

      <Section title="Headings">
        <Row label="H1 Count" value={result.h1Count} />
        <Row label="H2 Count" value={headingCounts.h2} />
        <Row label="H3 Count" value={headingCounts.h3} />
        <Row label="H4 Count" value={headingCounts.h4} />
        <Row label="H5 Count" value={headingCounts.h5} />
        <Row label="H6 Count" value={headingCounts.h6} />
      </Section>

      <Section title="Images">
        <Row label="Total Images" value={images.total} />
        <Row label="Missing Alt Text" value={images.missingAlt} />
        <Row label="Broken" value={images.broken} />
        <Row label="Large (>200 KB)" value={images.large} />
      </Section>

      <Section title="SEO Tags">
        <Row label="Canonical URL" value={seoTags.canonicalUrl || '(none)'} />
        <Row label="Viewport" value={seoTags.viewport || '(none)'} />
        <Row label="OG Title" value={seoTags.ogTitle || '(none)'} />
        <Row label="OG Description" value={seoTags.ogDescription || '(none)'} />
        <Row label="OG Image" value={seoTags.ogImage || '(none)'} />
      </Section>

      <Section title="Timing Breakdown">
        <Row label="Fetch Time" value={`${timing.fetchTimeMs} ms`} />
        <Row label="Processing Time" value={`${timing.processingTimeMs} ms`} />
        <Row label="Total Time" value={`${timing.totalTimeMs} ms`} />
      </Section>
    </div>
  )
}

export default AuditResult
