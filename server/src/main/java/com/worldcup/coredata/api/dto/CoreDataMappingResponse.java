package com.worldcup.coredata.api.dto;

public record CoreDataMappingResponse(
        Long id,
        Long importItemId,
        String targetType,
        Long targetId,
        String mappingStatus,
        String message
) {
}
