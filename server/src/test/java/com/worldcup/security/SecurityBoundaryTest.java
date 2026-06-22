package com.worldcup.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityBoundaryTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void publicGetNamespaceIsAnonymousButOnlyForReadMethods() throws Exception {
        mockMvc.perform(get("/api/public/not-yet-created"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/public/overview"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/import-jobs/scan"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void legacyRichReadEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/matches")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/odds")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/sentiment")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/prematch-workbench/matches")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/profiles/teams")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/core-data/overview")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/analysis-review/overview")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/import-items")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/system/settings")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanReadLegacyRichEndpoints() throws Exception {
        mockMvc.perform(get("/api/matches").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/odds").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/sentiment").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/prematch-workbench/matches").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/profiles/teams").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/core-data/overview").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/analysis-review/overview").with(httpBasic("admin", "admin123456"))).andExpect(status().isOk());
    }
}
