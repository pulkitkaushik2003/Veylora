package com.example.veylora.dto;

public record ContactImportResult(
        int importedCount,
        int duplicateCount,
        int skippedCount,
        int rowsProcessed
) {}
