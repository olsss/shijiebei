package com.worldcup.publicapi.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PublicApiMapper {
    private static final String FORBIDDEN_KEY_ALIASES = String.join("|",
            "rawJson", "raw_json", "rawPayload", "raw_payload", "raw", "payload",
            "archivePath", "archive_path", "sourcePath", "source_path",
            "ticketNo", "ticket_no",
            "stake", "stakeSuggestion", "stake_suggestion",
            "budgetAmount", "budget_amount", "returnAmount", "return_amount", "profitLoss", "profit_loss",
            "approvedBy", "approved_by", "reviewedBy", "reviewed_by", "reviewNote", "review_note",
            "mappings", "importItemId", "import_item_id"
    );

    private static final Pattern SENSITIVE_KEY_VALUE = Pattern.compile(
            "(?<![A-Za-z0-9_])\"?(?:" + FORBIDDEN_KEY_ALIASES + ")\"?\\s*[:=]\\s*"
                    + "(?:\"(?:\\\\.|[^\"])*\"|\\{[^{}]*}|[^\\s,;]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern WINDOWS_PATH = Pattern.compile("[A-Za-z]:[/\\\\][^\\s,;]+");

    public String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String sanitized = SENSITIVE_KEY_VALUE.matcher(value).replaceAll("[REDACTED]");
        sanitized = WINDOWS_PATH.matcher(sanitized).replaceAll("[REDACTED]");
        return sanitized;
    }
}
