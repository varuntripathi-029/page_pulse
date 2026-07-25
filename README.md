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

Page Pulse ships as a **single Spring Boot application**: in production, the backend jar serves the built React app as static content in addition to exposing the API. Locally, though, it's usually more convenient to run the two dev servers side by side (instant reload for the frontend, no rebuild needed for the backend). See [Production Build](#production-build--single-jar-deployment) below for how the two get combined.

### Backend (local dev)

Requires Java 21 and Maven.

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080` by default (configurable via the `PORT` environment variable).

### Frontend (local dev)

Requires Node.js 18+.

```bash
cd frontend
npm install
npm run dev
```

The app starts on `http://localhost:5173` by default, and proxies API calls to the backend running separately on port 8080.

**Configuration:** in dev mode only, the frontend needs to know where the backend is running. Copy `.env.example` to `.env` and set:

```
VITE_API_BASE_URL=http://localhost:8080
```

If this variable is not set, it defaults to `http://localhost:8080` for local development. This variable is **only read in dev mode** (`npm run dev`) — see [Production Build](#production-build--single-jar-deployment) for why the production build ignores it and always uses relative API paths instead.

## Production Build & Single-Jar Deployment

For deployment, `frontend/` and `backend/` are packaged into **one executable Spring Boot jar** that serves the React app and the REST API from the same port. There is no separate frontend host and no Docker involved.

### How it works

Running, from the `backend/` directory:

```bash
mvn clean package
```

automatically, with no manual steps:

1. **Installs Node/npm.** The `frontend-maven-plugin` downloads a pinned Node version into `frontend/node/` (isolated from any system-wide Node install — nothing needs to be preinstalled on the build machine).
2. **Builds the frontend.** It runs `npm ci` followed by `npm run build` inside `frontend/`, producing `frontend/dist/`.
3. **Copies the build output onto the classpath.** `maven-resources-plugin` copies `frontend/dist/**` into `target/classes/static` — the classpath location (`classpath:/static/`) that Spring Boot serves static content from automatically. This intentionally copies into the Maven **build output** directory rather than into the git-tracked `backend/src/main/resources/static/` source folder, so nothing generated ever gets committed to version control; `backend/target/` is already gitignored.
4. **Compiles and packages as usual.** `spring-boot-maven-plugin` repackages everything (backend classes + bundled static frontend + dependencies) into one fat jar: `target/page-pulse.jar`.

The result is a single self-contained artifact:

```bash
java -jar target/page-pulse.jar
```

starts one process that serves the React app at `http://localhost:8080/` **and** the API at `http://localhost:8080/api/audit`.

`frontend/` and `backend/` remain fully separate folders in source control — the frontend source is never merged into the backend. Only the compiled `frontend/dist` output crosses the boundary, and only at build time inside `target/`.

Frontend engineers can still build/preview the frontend completely independently of the backend at any time:

```bash
cd frontend
npm run build
npm run preview
```

### Routing: API vs. SPA fallback

Spring's `WebConfig` (`backend/src/main/java/com/digitalheroes/pagepulse/config/WebConfig.java`) registers a resource handler so that:

- `/api/**` is always routed to `AuditController` as before; an unmapped path under `/api/` returns a proper `404`, never the SPA's `index.html`.
- Any other path that matches a real file in the bundled static assets (JS/CSS/images) is served directly.
- Any other path that doesn't match a real file (e.g. the browser is refreshed on a client-side route) falls back to `index.html`, so refreshing the page never produces a 404.

### Why the frontend uses relative API paths in production

`frontend/src/api/auditApi.js` calls `fetch('/api/audit', ...)` with no host in production builds, since the frontend and API are now served from the same origin. In dev mode (`npm run dev`), it still targets `VITE_API_BASE_URL` (default `http://localhost:8080`) since the Vite dev server and the backend run as two separate processes on two different ports. This switch is driven by Vite's built-in `import.meta.env.DEV` flag rather than an environment variable, specifically so a stray `VITE_API_BASE_URL` left in `frontend/.env` (which Vite loads in *every* mode, including production builds) can never leak an absolute `localhost` URL into a deployed build.

## Deploying to Railway

This app deploys as a **single Railway service** — no Docker, no second service for the frontend.

1. Push the repository (with both `backend/` and `frontend/` folders) to GitHub.
2. Create a new Railway project from that repo.
3. In the service settings, set:
   - **Root Directory:** *(leave empty — repo root)*
   - **Build Command:** `mvn -f backend/pom.xml clean package -DskipTests`
   - **Start Command:** `java -jar backend/target/page-pulse.jar`
4. A `railpack.json` at the repo root pins the build to Railway's Java provider (`{"provider": "java"}`). This is required: Root Directory has to stay at the repo root so `frontend/` is included in the build context (`frontend-maven-plugin` reaches it via `../frontend` from `backend/pom.xml`), but Railpack's Java auto-detection only looks for a `pom.xml` at that root directory. Since `pom.xml` actually lives in `backend/`, Railpack would otherwise fall back to a plain shell environment with no JDK/Maven installed (`mvn: not found`). Forcing the provider explicitly sidesteps that detection gap while still giving the project the frontend directory it needs at build time.
5. Railway automatically injects a `PORT` environment variable, which `application.properties` already reads via `server.port=${PORT:8080}` — no configuration needed.
6. Deploy. Railway builds the jar (which builds the frontend as part of `mvn clean package`, per [Production Build](#production-build--single-jar-deployment) above) and starts it — you get one public URL serving both the UI and the API.

The `FRONTEND_ORIGIN`/`app.cors.allowed-origin` setting isn't needed for this deployment, since the frontend is served same-origin by the same service; it's only relevant for local dev, where it defaults to `http://localhost:5173`.

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

5. **`frontend-maven-plugin` over a separate frontend deployment.** Rather than deploying the React app to its own static host, the frontend build is triggered from `backend/pom.xml` and its output copied onto the backend's classpath at package time (see [Production Build](#production-build--single-jar-deployment)). This keeps deployment to a single Railway service with one URL and no Docker, while `frontend/` and `backend/` remain separate folders in source control — only the compiled `dist/` output crosses into `target/` at build time, never into git-tracked source.

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
- **Single-jar deployment.** The Maven build now bundles the React production build into the Spring Boot jar (see [Production Build](#production-build--single-jar-deployment)), so the whole app deploys as one Railway service with one URL, with no functional or UI changes to either side.
