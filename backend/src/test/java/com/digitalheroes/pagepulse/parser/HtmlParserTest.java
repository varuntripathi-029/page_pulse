package com.digitalheroes.pagepulse.parser;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlParserTest {

    private final HtmlParser htmlParser = new HtmlParser();

    @Test
    void happyPath_extractsAllMetricsCorrectly() {
        String html = """
                <html>
                <head>
                    <title>Example Domain</title>
                    <meta name="description" content="Example website for testing.">
                </head>
                <body>
                    <h1>Welcome</h1>
                    <h1>Second Heading</h1>
                    <img src="a.png" alt="A description">
                    <img src="b.png" alt="">
                    <img src="c.png">
                    <p>This is a simple paragraph with seven words.</p>
                </body>
                </html>
                """;

        Document document = htmlParser.parse(html, "https://example.com");

        assertEquals("Example Domain", htmlParser.extractTitle(document));
        assertEquals("Example website for testing.", htmlParser.extractMetaDescription(document));
        assertEquals(2, htmlParser.countH1Tags(document));
        assertEquals(2, htmlParser.countMissingAltImages(document));
        assertEquals(11, htmlParser.countWords(document));
    }

    @Test
    void missingTitleAndMetaDescription_returnsEmptyStrings() {
        String html = """
                <html>
                <head></head>
                <body>
                    <p>No title or meta description here.</p>
                </body>
                </html>
                """;

        Document document = htmlParser.parse(html, "https://example.com");

        assertEquals("", htmlParser.extractTitle(document));
        assertEquals("", htmlParser.extractMetaDescription(document));
    }

    @Test
    void malformedHtml_doesNotThrowAndParsesLeniently() {
        String malformedHtml = """
                <html><head><title>Broken Page
                <body>
                    <h1>Heading without closing tag
                    <p>Unclosed paragraph <img src="x.png">
                """;

        assertDoesNotThrow(() -> {
            Document document = htmlParser.parse(malformedHtml, "https://example.com");
            htmlParser.extractTitle(document);
            htmlParser.extractMetaDescription(document);
            htmlParser.countH1Tags(document);
            htmlParser.countMissingAltImages(document);
            htmlParser.countWords(document);
        });
    }

    @Test
    void extractHeadingCounts_countsEachLevelIndependently() {
        String html = """
                <html><body>
                    <h1>Title</h1>
                    <h2>Section A</h2>
                    <h2>Section B</h2>
                    <h3>Sub A</h3>
                    <h6>Fine print</h6>
                </body></html>
                """;

        Document document = htmlParser.parse(html, "https://example.com");
        var headingCounts = htmlParser.extractHeadingCounts(document);

        assertEquals(2, headingCounts.h2());
        assertEquals(1, headingCounts.h3());
        assertEquals(0, headingCounts.h4());
        assertEquals(0, headingCounts.h5());
        assertEquals(1, headingCounts.h6());
    }

    @Test
    void extractImageUrls_resolvesAbsoluteUrlsAndRespectsLimit() {
        String html = """
                <html><body>
                    <img src="/a.png">
                    <img src="https://cdn.example.com/b.png">
                    <img src="c.png">
                </body></html>
                """;

        Document document = htmlParser.parse(html, "https://example.com/page/");
        var urls = htmlParser.extractImageUrls(document, 2);

        assertEquals(2, urls.size());
        assertEquals("https://example.com/a.png", urls.get(0));
        assertEquals("https://cdn.example.com/b.png", urls.get(1));
    }

    @Test
    void extractSeoTags_readsCanonicalViewportAndOpenGraphTags() {
        String html = """
                <html><head>
                    <link rel="canonical" href="/canonical-page">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <meta property="og:title" content="OG Title">
                    <meta property="og:description" content="OG Description">
                    <meta property="og:image" content="https://example.com/og.png">
                </head><body></body></html>
                """;

        Document document = htmlParser.parse(html, "https://example.com");
        var seoTags = htmlParser.extractSeoTags(document);

        assertEquals("https://example.com/canonical-page", seoTags.canonicalUrl());
        assertEquals("width=device-width, initial-scale=1", seoTags.viewport());
        assertEquals("OG Title", seoTags.ogTitle());
        assertEquals("OG Description", seoTags.ogDescription());
        assertEquals("https://example.com/og.png", seoTags.ogImage());
    }

    @Test
    void extractSeoTags_returnsEmptyStringsWhenTagsAreAbsent() {
        String html = "<html><head></head><body></body></html>";

        Document document = htmlParser.parse(html, "https://example.com");
        var seoTags = htmlParser.extractSeoTags(document);

        assertEquals("", seoTags.canonicalUrl());
        assertEquals("", seoTags.viewport());
        assertEquals("", seoTags.ogTitle());
        assertEquals("", seoTags.ogDescription());
        assertEquals("", seoTags.ogImage());
    }

    @Test
    void wordCount_ignoresScriptStyleAndNoscriptContent() {
        String html = """
                <html>
                <head>
                    <style>body { color: red; }</style>
                </head>
                <body>
                    <script>console.log('should not be counted at all');</script>
                    <noscript>Enable JavaScript to continue</noscript>
                    <p>Only these four words count.</p>
                </body>
                </html>
                """;

        Document document = htmlParser.parse(html, "https://example.com");

        assertEquals(5, htmlParser.countWords(document));
    }
}
