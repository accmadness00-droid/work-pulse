package uz.workpulse.auth.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.workpulse.auth.domain.RefreshToken;
import uz.workpulse.auth.domain.User;
import uz.workpulse.auth.dto.AuthResponse;
import uz.workpulse.auth.dto.ChangePasswordRequest;
import uz.workpulse.auth.dto.LoginRequest;
import uz.workpulse.auth.dto.LogoutRequest;
import uz.workpulse.auth.dto.MeResponse;
import uz.workpulse.auth.dto.RefreshTokenRequest;
import uz.workpulse.auth.infrastructure.RefreshTokenRepository;
import uz.workpulse.auth.infrastructure.UserRepository;
import uz.workpulse.employee.application.EmployeeFacade;
import uz.workpulse.shared.config.JwtConfig;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AuthPrincipal;
import uz.workpulse.shared.security.TokenBlacklistService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtConfig jwtConfig;
    private final EmployeeFacade employeeFacade;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordService passwordService,
            JwtService jwtService,
            TokenBlacklistService tokenBlacklistService,
            JwtConfig jwtConfig,
            EmployeeFacade employeeFacade
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtConfig = jwtConfig;
        this.employeeFacade = employeeFacade;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordService.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        ensureActive(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        JwtService.JwtClaims claims = jwtService.validateRefreshToken(request.refreshToken());
        String tokenHash = jwtService.hashToken(request.refreshToken());

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_JWT));
        if (refreshToken.isRevoked()) {
            throw new BusinessException(ErrorCode.REVOKED_REFRESH_TOKEN);
        }
        if (Instant.now().isAfter(refreshToken.getExpiresAt())) {
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }
        if (!refreshToken.getUserId().equals(claims.userId())) {
            throw new BusinessException(ErrorCode.INVALID_JWT);
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_JWT));
        ensureActive(user);

        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
        return issueTokens(user);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String refreshTokenHash = jwtService.hashToken(request.refreshToken());
        refreshTokenRepository.findByTokenHash(refreshTokenHash).ifPresent(refreshToken -> {
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);
        });

        if (StringUtils.hasText(request.accessToken())) {
            blacklistAccessToken(request.accessToken());
        }
    }

    public void blacklistAccessToken(String accessToken) {
        JwtService.JwtClaims claims = jwtService.validateAccessToken(accessToken);
        Duration ttl = Duration.between(Instant.now(), claims.expiresAt());
        tokenBlacklistService.blacklist(jwtService.hashToken(accessToken), ttl);
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        User user = getCurrentUser();
        ensureActive(user);
        UUID employeeId = employeeFacade.findEmployeeIdByUserId(user.getId()).orElse(null);
        return MeResponse.from(user, employeeId);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();
        ensureActive(user);
        if (!passwordService.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        user.setPasswordHash(passwordService.encode(request.newPassword()));
        userRepository.save(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        Instant refreshExpiresAt = jwtService.validateRefreshToken(refreshToken).expiresAt();

        refreshTokenRepository.save(new RefreshToken(user.getId(), jwtService.hashToken(refreshToken), refreshExpiresAt));
        return new AuthResponse(accessToken, refreshToken, "Bearer");
    }

    private User getCurrentUser() {
        AuthPrincipal principal = currentPrincipal();
        return userRepository.findById(principal.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_JWT));
    }

    private AuthPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new BusinessException(ErrorCode.INVALID_JWT);
        }
        return principal;
    }

    private void ensureActive(User user) {
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.INACTIVE_USER);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
