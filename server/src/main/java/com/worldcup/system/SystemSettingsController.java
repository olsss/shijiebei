package com.worldcup.system;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.config.AppProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemSettingsController {
    private final AppProperties appProperties;

    public SystemSettingsController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @GetMapping("/settings")
    public ApiResponse<SystemSettingsResponse> settings() {
        return ApiResponse.ok(new SystemSettingsResponse(
                appProperties.getArchivePath(),
                true,
                "Java 系统只管理已批准数据，不替代 skill/ 比赛分析流程"
        ));
    }

    public record SystemSettingsResponse(
            String archivePath,
            boolean analysisSystemProtected,
            String boundaryDescription
    ) {
    }
}
