package com.worldcup.auth;

public record LoginResponse(String username, String displayName, String authType) {
}
