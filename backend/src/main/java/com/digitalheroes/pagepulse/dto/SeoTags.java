package com.digitalheroes.pagepulse.dto;

public record SeoTags(
        String canonicalUrl,
        String viewport,
        String ogTitle,
        String ogDescription,
        String ogImage
) {
}
