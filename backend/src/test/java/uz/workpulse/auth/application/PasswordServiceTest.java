package uz.workpulse.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService(new BCryptPasswordEncoder(12));

    @Test
    void encodeAndMatches() {
        String encoded = passwordService.encode("admin123");

        assertThat(encoded).isNotEqualTo("admin123");
        assertThat(passwordService.matches("admin123", encoded)).isTrue();
        assertThat(passwordService.matches("wrong", encoded)).isFalse();
    }
}
