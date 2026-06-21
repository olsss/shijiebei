package com.worldcup.auth;

import com.worldcup.common.api.ApiResponse;
import com.worldcup.config.AppProperties;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppProperties appProperties;

    public AuthController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        var admin = appProperties.getAdmin();
        boolean usernameMatches = admin.getUsername().equals(request.username());
        boolean passwordMatches = admin.getPassword().equals(request.password());

        if (!usernameMatches || !passwordMatches) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("用户名或密码错误"));
        }

        return ResponseEntity.ok(ApiResponse.ok(new LoginResponse(admin.getUsername(), "系统管理员", "basic")));
    }
}
