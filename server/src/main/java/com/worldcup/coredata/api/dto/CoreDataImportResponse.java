package com.worldcup.coredata.api.dto;

import java.util.List;

public record CoreDataImportResponse(
        Long importItemId,
        String status,
        String message,
        List<CoreDataMappingResponse> mappings
) {
}
