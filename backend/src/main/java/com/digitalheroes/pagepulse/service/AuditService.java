package com.digitalheroes.pagepulse.service;

import com.digitalheroes.pagepulse.dto.AuditResponse;
import com.digitalheroes.pagepulse.dto.HeadingCounts;
import com.digitalheroes.pagepulse.dto.ImageAudit;
import com.digitalheroes.pagepulse.dto.SeoTags;
import com.digitalheroes.pagepulse.dto.Timing;
import com.digitalheroes.pagepulse.exception.InvalidUrlException;
import com.digitalheroes.pagepulse.exception.NonHtmlResponseException;
import com.digitalheroes.pagepulse.exception.RequestTimeoutException;
import com.digitalheroes.pagepulse.exception.UrlFetchException;
import com.digitalheroes.pagepulse.parser.HtmlParser;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
public class AuditService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(6);
    private static final int MAX_IMAGES_TO_INSPECT = 8;

    private final HtmlParser htmlParser;
    private final ImageAuditor imageAuditor;
    private final HttpClient httpClient;

    public AuditService(HtmlParser htmlParser, ImageAuditor imageAuditor, HttpClient httpClient) {
        this.htmlParser = htmlParser;
        this.imageAuditor = imageAuditor;
        this.httpClient = httpClient;
    }

    public AuditResponse audit(String rawUrl) {
        URI uri = validateAndParseUrl(rawUrl);

        long fetchStart = System.currentTimeMillis();
        HttpResponse<String> response = fetch(uri);
        long fetchTimeMs = System.currentTimeMillis() - fetchStart;

        validateHtmlContentType(response);

        long processingStart = System.currentTimeMillis();

        Document document = htmlParser.parse(response.body(), uri.toString());

        int totalImages = htmlParser.countTotalImages(document);
        int missingAltImages = htmlParser.countMissingAltImages(document);
        List<String> imageUrls = htmlParser.extractImageUrls(document, MAX_IMAGES_TO_INSPECT);
        ImageAudit imageAudit = imageAuditor.inspect(imageUrls, totalImages, missingAltImages);

        HeadingCounts headingCounts = htmlParser.extractHeadingCounts(document);
        SeoTags seoTags = htmlParser.extractSeoTags(document);
        long pageSizeBytes = response.body().getBytes(StandardCharsets.UTF_8).length;

        long processingTimeMs = System.currentTimeMillis() - processingStart;
        long totalTimeMs = fetchTimeMs + processingTimeMs;

        return new AuditResponse(
                response.statusCode(),
                totalTimeMs,
                htmlParser.extractTitle(document),
                htmlParser.extractMetaDescription(document),
                htmlParser.countH1Tags(document),
                missingAltImages,
                htmlParser.countWords(document),
                headingCounts,
                imageAudit,
                seoTags,
                pageSizeBytes,
                new Timing(fetchTimeMs, processingTimeMs, totalTimeMs)
        );
    }

    private URI validateAndParseUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new InvalidUrlException("URL must not be empty");
        }

        try {
            URI uri = new URI(rawUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();

            boolean hasValidScheme = scheme != null
                    && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));

            if (!hasValidScheme || host == null || host.isBlank()) {
                throw new InvalidUrlException("URL must be a valid http or https address");
            }

            return uri;
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("URL is malformed");
        }
    }

    private HttpResponse<String> fetch(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "PagePulse/1.0")
                .GET()
                .build();

        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new RequestTimeoutException("Request timed out");
        } catch (UnknownHostException e) {
            throw new UrlFetchException("Unable to resolve host", e);
        } catch (IOException e) {
            throw new UrlFetchException("Unable to fetch URL", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UrlFetchException("Request was interrupted", e);
        }
    }

    private void validateHtmlContentType(HttpResponse<String> response) {
        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("")
                .toLowerCase(Locale.ROOT);

        if (!contentType.contains("text/html")) {
            throw new NonHtmlResponseException("URL does not point to an HTML page");
        }
    }
}
