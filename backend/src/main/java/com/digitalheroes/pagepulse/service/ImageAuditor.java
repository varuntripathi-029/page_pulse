package com.digitalheroes.pagepulse.service;

import com.digitalheroes.pagepulse.dto.ImageAudit;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Performs bounded HEAD-request checks against a page's images to flag
 * broken links and unusually large files, without materially slowing down
 * the overall audit. Only the first {@link #MAX_IMAGES_CHECKED} distinct
 * image URLs are inspected.
 */
@Component
public class ImageAuditor {

    static final int MAX_IMAGES_CHECKED = 8;
    static final long LARGE_IMAGE_THRESHOLD_BYTES = 200_000L;
    private static final Duration IMAGE_CHECK_TIMEOUT = Duration.ofMillis(1200);

    private final HttpClient httpClient;

    public ImageAuditor(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public ImageAudit inspect(List<String> imageUrls, int totalImages, int missingAltCount) {
        int checked = 0;
        int broken = 0;
        int large = 0;

        for (String imageUrl : imageUrls) {
            if (checked >= MAX_IMAGES_CHECKED) {
                break;
            }
            checked++;

            ImageCheckResult result = checkImage(imageUrl);
            if (result.broken()) {
                broken++;
            }
            if (result.large()) {
                large++;
            }
        }

        return new ImageAudit(totalImages, missingAltCount, broken, large);
    }

    private ImageCheckResult checkImage(String imageUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(IMAGE_CHECK_TIMEOUT)
                    .header("User-Agent", "PagePulse/1.0")
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            boolean broken = response.statusCode() >= 400;
            boolean large = response.headers()
                    .firstValueAsLong("Content-Length")
                    .orElse(0L) > LARGE_IMAGE_THRESHOLD_BYTES;

            return new ImageCheckResult(broken, large);
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new ImageCheckResult(true, false);
        }
    }

    private record ImageCheckResult(boolean broken, boolean large) {
    }
}
