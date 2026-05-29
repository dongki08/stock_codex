package com.parkdh.stockadvisor.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {
    @Test
    void appSecurityPropertiesFiltersBlankAndWildcardOrigins() {
        AppSecurityProperties properties = new AppSecurityProperties(List.of(" ", "*", "https://stock.example.com"), true);

        assertThat(properties.allowedOrigins()).containsExactly("https://stock.example.com");
    }

    @Test
    void userDetailsStoresAdminPasswordWithBCrypt() {
        SecurityConfig securityConfig = new SecurityConfig(
                new AdminProperties("admin", "change-me"),
                new AppSecurityProperties(List.of("http://localhost:5173"), true)
        );

        PasswordEncoder encoder = securityConfig.passwordEncoder();
        UserDetailsService userDetailsService = securityConfig.userDetailsService(encoder);

        UserDetails admin = userDetailsService.loadUserByUsername("admin");

        assertThat(admin.getPassword()).doesNotStartWith("{noop}");
        assertThat(encoder.matches("change-me", admin.getPassword())).isTrue();
    }
}
