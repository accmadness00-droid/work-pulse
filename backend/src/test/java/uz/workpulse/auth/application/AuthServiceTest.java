package uz.workpulse.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import uz.workpulse.auth.domain.RefreshToken;
import uz.workpulse.auth.domain.User;
import uz.workpulse.auth.dto.AuthResponse;
import uz.workpulse.auth.dto.LoginRequest;
import uz.workpulse.auth.dto.LogoutRequest;
import uz.workpulse.auth.infrastructure.RefreshTokenRepository;
import uz.workpulse.auth.infrastructure.UserRepository;
import uz.workpulse.employee.application.EmployeeFacade;
import uz.workpulse.shared.config.JwtConfig;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.TokenBlacklistService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private EmployeeFacade employeeFacade;

    private PasswordService passwordService;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService(new BCryptPasswordEncoder(12));
        jwtService = new JwtService(new JwtConfig("work-pulse-test", "test-secret-that-is-long-enough-for-hmac", 15, 7));
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordService,
                jwtService,
                tokenBlacklistService,
                new JwtConfig("work-pulse-test", "test-secret-that-is-long-enough-for-hmac", 15, 7),
                employeeFacade
        );
    }

    @Test
    void loginHappyPath() {
        User user = user(passwordService.encode("admin123"));
        when(userRepository.findByEmail("admin@workpulse.uz")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(new LoginRequest("admin@workpulse.uz", "admin123"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(jwtService.validateAccessToken(response.accessToken()).userId()).isEqualTo(user.getId());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void loginWithInvalidPasswordFails() {
        User user = user(passwordService.encode("admin123"));
        when(userRepository.findByEmail("admin@workpulse.uz")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@workpulse.uz", "wrong")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    private User user(String passwordHash) {
        User user = new User("admin@workpulse.uz", passwordHash, User.Role.SUPER_ADMIN, true);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
