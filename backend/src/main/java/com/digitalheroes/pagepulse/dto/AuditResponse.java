package com.digitalheroes.pagepulse.dto;

public record AuditResponse(
        int status,
        long responseTime,
        String title,
        String metaDescription,
        int h1Count,
        int missingAltImages,
        int wordCount,
        HeadingCounts headingCounts,
        ImageAudit images,
        SeoTags seoTags,
        long pageSizeBytes,
        Timing timing
) {
}
