package com.connect.config;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.connect.service.FileStorageService;

/**
 * Web configuration.
 *
 * <p>Exposes a {@link CorsConfigurationSource} so the Spring Security filter
 * chain allows the Vite frontend (default http://localhost:5173) to call the
 * API. Allowed origins are configurable via {@code app.cors.allowed-origins}.
 * Also serves uploaded profile pictures from the local uploads directory at
 * {@code /uploads/**}.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;
    private final String uploadsDir;

    public WebConfig(@Value("${app.cors.allowed-origins:http://localhost:5173}") String[] allowedOrigins,
                     @Value("${app.uploads.dir:uploads}") String uploadsDir) {
        this.allowedOrigins = Arrays.asList(allowedOrigins);
        this.uploadsDir = uploadsDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadsDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(FileStorageService.PUBLIC_PREFIX + "**")
                .addResourceLocations(location);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
