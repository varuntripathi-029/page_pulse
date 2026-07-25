package com.digitalheroes.pagepulse.parser;

import com.digitalheroes.pagepulse.dto.HeadingCounts;
import com.digitalheroes.pagepulse.dto.SeoTags;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Extracts audit metrics from raw HTML content.
 */
@Component
public class HtmlParser {

    public String extractTitle(Document document) {
        String title = document.title();
        return title == null ? "" : title.trim();
    }

    public String extractMetaDescription(Document document) {
        return extractMetaContent(document, "meta[name=description]");
    }

    public int countH1Tags(Document document) {
        return countTags(document, "h1");
    }

    public HeadingCounts extractHeadingCounts(Document document) {
        return new HeadingCounts(
                countTags(document, "h2"),
                countTags(document, "h3"),
                countTags(document, "h4"),
                countTags(document, "h5"),
                countTags(document, "h6")
        );
    }

    public int countTotalImages(Document document) {
        return document.select("img").size();
    }

    public int countMissingAltImages(Document document) {
        Elements images = document.select("img");
        int missingCount = 0;
        for (Element image : images) {
            String alt = image.attr("alt");
            if (alt == null || alt.trim().isEmpty()) {
                missingCount++;
            }
        }
        return missingCount;
    }

    /**
     * Returns distinct, absolute image URLs in document order, capped at {@code limit}.
     * Used by the service layer to perform bounded network checks for broken/large images.
     */
    public List<String> extractImageUrls(Document document, int limit) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> urls = new ArrayList<>();

        for (Element image : document.select("img")) {
            if (urls.size() >= limit) {
                break;
            }
            String absoluteUrl = image.absUrl("src");
            if (!absoluteUrl.isBlank() && seen.add(absoluteUrl)) {
                urls.add(absoluteUrl);
            }
        }
        return urls;
    }

    public int countWords(Document document) {
        Document clone = document.clone();
        clone.select("script, style, noscript").remove();

        String visibleText = clone.body() != null ? clone.body().text() : "";
        String trimmedText = visibleText.trim();

        if (trimmedText.isEmpty()) {
            return 0;
        }
        return trimmedText.split("\\s+").length;
    }

    public SeoTags extractSeoTags(Document document) {
        return new SeoTags(
                extractCanonicalUrl(document),
                extractMetaContent(document, "meta[name=viewport]"),
                extractMetaContent(document, "meta[property=og:title]"),
                extractMetaContent(document, "meta[property=og:description]"),
                extractMetaContent(document, "meta[property=og:image]")
        );
    }

    public Document parse(String html, String baseUri) {
        return Jsoup.parse(html, baseUri);
    }

    private String extractCanonicalUrl(Document document) {
        Element link = document.selectFirst("link[rel=canonical]");
        if (link == null) {
            return "";
        }
        String href = link.absUrl("href");
        return href == null ? "" : href.trim();
    }

    private String extractMetaContent(Document document, String cssSelector) {
        Element tag = document.selectFirst(cssSelector);
        if (tag == null) {
            return "";
        }
        String content = tag.attr("content");
        return content == null ? "" : content.trim();
    }

    private int countTags(Document document, String tagName) {
        return document.select(tagName).size();
    }
}
