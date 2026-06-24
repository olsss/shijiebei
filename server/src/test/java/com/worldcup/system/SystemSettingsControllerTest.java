package com.worldcup.system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemSettingsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void settingsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/system/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void settingsReturnArchivePath() throws Exception {
        mockMvc.perform(get("/api/system/settings").with(httpBasic("admin", "admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.archivePath").value("../data-inbox/pending"))
                .andExpect(jsonPath("$.data.analysisSystemProtected").value(true));
    }
}
