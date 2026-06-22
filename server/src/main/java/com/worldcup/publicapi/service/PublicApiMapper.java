package com.worldcup.publicapi.service;

import org.springframework.stereotype.Component;

@Component
public class PublicApiMapper {
    public String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String sanitized = value;
        sanitized = sanitized.replaceAll("(?i)ticketNo\\s*[:=]\\s*[^\\s,;]+", "ticketNo=[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)(stake|stakeSuggestion|budgetAmount|returnAmount|profitLoss)\\s*[:=]\\s*[-+]?[0-9]+(\\.[0-9]+)?", "$1=[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)(reviewedBy|approvedBy|reviewNote)\\s*[:=]\\s*[^\\s,;]+", "$1=[REDACTED]");
        sanitized = sanitized.replaceAll("[A-Za-z]:[/\\\\][^\\s,;]+", "[REDACTED]");
        sanitized = sanitized.replaceAll("\\{[^{}]*(raw|payload)[^{}]*}", "[REDACTED]");
        return sanitized;
    }
}