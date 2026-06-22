package com.worldcup.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class AppPropertiesSecurityTest {
    @Test
    void appPropertiesDoNotProvideDefaultAdminPassword() {
        AppProperties properties = new AppProperties();

        assertThat(properties.getAdmin().getPassword()).isNull();

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var violations = validatorFactory.getValidator().validate(properties);
            assertThat(violations)
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("admin.password"));
        }
    }

    @Test
    void mainApplicationConfigDoesNotShipDefaultAdminPassword() throws IOException {
        try (var stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("application.yml"),
                "application.yml must be on the test classpath"
        )) {
            String yaml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(yaml).doesNotContain("admin123456");
            assertThat(yaml).doesNotContainPattern("(?m)^\\s*password:\\s*admin123456\\s*$");
        }
    }
}
