package uz.workpulse.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtConfig(
        String issuer,
        String secret,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays
) {
}
