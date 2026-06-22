package com.worldcup.coredata.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MatchKeyNormalizer {
    public String normalize(String matchName, String matchday, String jcCode) {
        String base = Stream.of(matchName, matchday, jcCode)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("|"));
        return base.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
