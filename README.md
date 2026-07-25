# Page Pulse

Page Pulse is a full-stack web application that audits any public webpage by URL. A user submits a URL from the frontend, the Spring Boot backend fetches and analyzes the page's HTML, and the results — status, timing, content metrics, heading structure, image health, and SEO tags — are returned as JSON and rendered in a clean, single-page UI.

## Features

- Audit any webpage by URL
- Display HTTP status
- Display response time (with a fetch/processing timing breakdown)
- Extract page title
- Extract meta description
- Count H1 tags (plus H2–H6 heading structure)
- Count images missing alt attributes (plus total image count, broken images, and unusually large images)
- Approximate visible word count
- Page size in bytes
- Basic SEO tag extraction (canonical URL, viewport, Open Graph title/description/image)
- Proper error handling for invalid URLs, timeouts, connection failures, and non-HTML responses

## Tech Stack

**Backend:**
- Java 21
- Spring Boot 3.x
- Maven
- Java 21 `HttpClient` (native, no RestTemplate)
- Jsoup
- JUnit 5

**Frontend:**
- React
- Vite
- Tailwind CSS v4

## Project Structure

```
page-pulse/
├── backend/
│   └── src/main/java/com/digitalheroes/pagepulse/
│       ├── controller/   # HTTP layer only — receives requests, delegates to the service
│       ├── service/      # Business logic: URL validation, fetching, orchestration
│       ├── parser/       # Pure HTML parsing/extraction logic (no I/O)
│       ├── dto/          # Request/response records exchanged with the frontend
│       ├── exception/    # Custom exceptions + centralized exception handler
│       └── config/       # Shared beans (HttpClient) and CORS configuration
└── frontend/
    └── src/
        ├── api/          # Thin fetch wrapper around the backend API
        ├── components/   # AuditForm, AuditResult, ErrorCard, Footer
        └── App.jsx        # Top-level state and layout
```

Each backend package has a single responsibility: controllers never contain business logic, the service layer never parses HTML directly, and the parser never performs network I/O.

## Setup Instructions

### Backend

Requires Java 21 and Maven.

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080` by default (configurable via the `PORT` environment variable).

### Frontend

Requires Node.js 18+.

```bash
cd frontend
npm install
npm run dev
```

The app starts on `http://localhost:5173` by default.

**Configuration:** the frontend needs to know where the backend is running. Copy `.env.example` to `.env` and set:

```
VITE_API_BASE_URL=http://localhost:8080
```

If this variable is not set, it defaults to `http://localhost:8080` for local development.

## API Contract

### Endpoint

```
POST /api/audit
```

### Request

```json
{
  "url": "https://example.com"
}
```

### Successful Response

```json
{
  "status": 200,
  "responseTime": 215,
  "title": "Example Domain",
  "metaDescription": "Example website...",
  "h1Count": 1,
  "missingAltImages": 2,
  "wordCount": 420,
  "headingCounts": {
    "h2": 3,
    "h3": 5,
    "h4": 0,
    "h5": 0,
    "h6": 0
  },
  "images": {
    "total": 6,
    "missingAlt": 2,
    "broken": 0,
    "large": 1
  },
  "seoTags": {
    "canonicalUrl": "https://example.com/",
    "viewport": "width=device-width, initial-scale=1",
    "ogTitle": "Example Domain",
    "ogDescription": "Example website...",
    "ogImage": "https://example.com/og.png"
  },
  "pageSizeBytes": 187342,
  "timing": {
    "fetchTimeMs": 180,
    "processingTimeMs": 35,
    "totalTimeMs": 215
  }
}
```

`status`, `responseTime`, `title`, `metaDescription`, `h1Count`, `missingAltImages`, and `wordCount` are kept at the top level of the response for backward compatibility with the original API contract; the remaining fields are additive.

### Error Responses

**400 — Invalid URL**
```json
{
  "error": "Invalid URL"
}
```

**502 — Unable to fetch URL** (DNS failure or connection refused)
```json
{
  "error": "Unable to fetch URL"
}
```

**504 — Request timed out**
```json
{
  "error": "Request timed out"
}
```

**415 — Non-HTML response**
```json
{
  "error": "URL does not point to an HTML page"
}
```

**500 — Internal server error**
```json
{
  "error": "Internal server error"
}
```

## Running Tests

JUnit 5 tests cover the parsing logic in isolation (no network calls).

```bash
cd backend
mvn test
```

Tests include: happy-path extraction of every metric, empty-string fallback when title/meta description are missing, graceful handling of malformed/unclosed HTML, heading-level counting, absolute image URL resolution with limits, SEO tag extraction (present and absent), and word-count exclusion of `script`/`style`/`noscript` content.

## Design Decisions

1. **Java `HttpClient` instead of `RestTemplate`.** `RestTemplate` is in maintenance mode and Spring's own docs point new code toward `RestClient` or native alternatives. Java 21's built-in `HttpClient` needs no extra dependency, supports per-request timeouts natively, and is a better fit for a lightweight service that only needs to issue simple GET/HEAD requests.

2. **Jsoup for HTML parsing.** Real-world webpages are frequently malformed — unclosed tags, missing quotes, invalid nesting. Jsoup parses leniently like a browser would, exposes a familiar CSS-selector API for pulling out titles, meta tags, and headings, and never throws on broken markup, which matches the "must never crash" requirement directly.

3. **Parsing logic separated into its own `HtmlParser` class.** Keeping all Jsoup logic in a dedicated, stateless component (rather than inline in the service or controller) makes it independently unit-testable with plain HTML strings and no network or Spring context required. It also keeps `AuditService` focused purely on orchestration: validate → fetch → parse → assemble response.

4. **A single shared `HttpClient` bean.** Both the main page fetch and the new image-checking feature reuse one `HttpClient` instance (configured in `HttpClientConfig`) rather than constructing a new client per request. This avoids the overhead of repeated client setup and keeps connection-pooling behavior consistent across the service.

## Deployment

### Backend on Render

1. Push the `backend/` folder to a Git repository.
2. Create a new **Web Service** on Render, pointing at the repo with `backend/` as the root directory.
3. Build command: `mvn clean package -DskipTests`
4. Start command: `java -jar target/page-pulse.jar`
5. Render automatically provides a `PORT` environment variable, which the app already reads (`server.port=${PORT:8080}`).
6. Set the `FRONTEND_ORIGIN` environment variable to the deployed Vercel URL (e.g. `https://page-pulse.vercel.app`) so CORS allows requests from the frontend.

### Frontend on Vercel

1. Push the `frontend/` folder to a Git repository.
2. Import the project into Vercel, with `frontend/` as the root directory.
3. Framework preset: **Vite**.
4. Set the environment variable `VITE_API_BASE_URL` to the deployed Render backend URL (e.g. `https://page-pulse-backend.onrender.com`).
5. Deploy — Vercel runs `npm install` and `npm run build` automatically.

## Future Improvements

The following were intentionally left out to stay within the scope of the assignment:

- Caching repeated URL requests to avoid re-fetching identical pages
- Better text extraction for more accurate word counting (e.g. ignoring navigation/footer boilerplate)
- Support for redirects and redirect history
- Additional SEO metrics (structured data, robots meta tags, hreflang)
- Concurrent fetching of images for faster broken/large-image detection on image-heavy pages
- Integration tests covering the full HTTP request/response cycle
- Persisted audit history

## Footer Requirement

Every page includes the required footer text, **"Built for Digital Heroes Training Task,"** linked to [https://digitalheroesco.com](https://digitalheroesco.com).

## What Was Added Beyond the Original Spec

After the initial four-step build, the following were added at your request:

- **More audit metrics:** H2–H6 heading counts, total image count, broken-image detection, large-image detection (>200 KB), page size in bytes, and basic SEO tag extraction (canonical URL, viewport, Open Graph title/description/image).
- **A fetch/processing timing breakdown** (`timing.fetchTimeMs` / `processingTimeMs` / `totalTimeMs`) in addition to the original single `responseTime` value.
- **Backend speed tuning:** the HTTP connect timeout was trimmed from 5s to 3s and the page-fetch request timeout from 10s to 6s. A new bounded `ImageAuditor` checks at most 8 distinct image URLs via `HEAD` requests with a 1.2s timeout each, so broken/large-image detection can't meaningfully slow down the overall audit. A single shared `HttpClient` bean replaced the previous per-service client.
- **Frontend restyle to Tailwind CSS v4**, using a white background, black text, and light-grey cards so text stays high-contrast, plus new result sections (Headings, Images, SEO Tags, Timing Breakdown) to surface the new metrics — no other UI or layout changes.
