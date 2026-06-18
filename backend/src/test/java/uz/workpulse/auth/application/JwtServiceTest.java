package uz.workpulse.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uz.workpulse.auth.domain.User;
import uz.workpulse.shared.config.JwtConfig;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new JwtConfig("work-pulse-test", "test-secret-that-is-long-enough-for-hmac", 15, 7)
    );

    @Test
    void generateAndValidateAccessToken() {
        User user = user();

        String token = jwtService.generateAccessToken(user);
        JwtService.JwtClaims claims = jwtService.validateAccessToken(token);

        assertThat(claims.userId()).isEqualTo(user.getId());
        assertThat(claims.email()).isEqualTo("admin@workpulse.uz");
        assertThat(claims.role()).isEqualTo(User.Role.SUPER_ADMIN);
        assertThat(claims.tokenType()).isEqualTo("access");
    }

    @Test
    void generateAndValidateRefreshToken() {
        User user = user();

        String token = jwtService.generateRefreshToken(user);
        JwtService.JwtClaims claims = jwtService.validateRefreshToken(token);

        assertThat(claims.userId()).isEqualTo(user.getId());
        assertThat(claims.tokenType()).isEqualTo("refresh");
        assertThat(jwtService.hashToken(token)).isNotEqualTo(token);
    }

    private User user() {
        User user = new User("admin@workpulse.uz", "hash", User.Role.SUPER_ADMIN, true);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
