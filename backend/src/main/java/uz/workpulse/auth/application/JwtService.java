package uz.workpulse.auth.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import uz.workpulse.auth.domain.User;
import uz.workpulse.shared.config.JwtConfig;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    private final JwtConfig jwtConfig;
    private final ObjectMapper objectMapper;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.objectMapper = new ObjectMapper();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtConfig.accessTokenTtlMinutes() * 60);
        return generateToken(user, ACCESS_TYPE, now, expiresAt);
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = getRefreshTokenExpiresAt(now);
        return generateToken(user, REFRESH_TYPE, now, expiresAt);
    }

    public Instant getRefreshTokenExpiresAt(Instant issuedAt) {
        return issuedAt.plusSeconds(jwtConfig.refreshTokenTtlDays() * 24 * 60 * 60);
    }

    public JwtClaims validateAccessToken(String token) {
        JwtClaims claims = validateToken(token);
        if (!ACCESS_TYPE.equals(claims.tokenType())) {
            throw new BusinessException(ErrorCode.INVALID_JWT);
        }
        return claims;
    }

    public JwtClaims validateRefreshToken(String token) {
        JwtClaims claims = validateToken(token);
        if (!REFRESH_TYPE.equals(claims.tokenType())) {
            throw new BusinessException(ErrorCode.INVALID_JWT);
        }
        return claims;
    }

    public JwtClaims validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException(ErrorCode.INVALID_JWT);
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signingInput);
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new BusinessException(ErrorCode.INVALID_JWT);
            }

            Map<String, Object> payload = readJson(parts[1]);
            if (!jwtConfig.issuer().equals(payload.get("iss"))) {
                throw new BusinessException(ErrorCode.INVALID_JWT);
            }

            long expiresAt = asLong(payload.get("exp"));
            if (Instant.now().getEpochSecond() >= expiresAt) {
                throw new BusinessException(ErrorCode.INVALID_JWT);
            }

            return new JwtClaims(
                    UUID.fromString((String) payload.get("sub")),
                    (String) payload.get("email"),
                    User.Role.valueOf((String) payload.get("role")),
                    (String) payload.get("typ"),
                    Instant.ofEpochSecond(expiresAt),
                    parseUuid(payload.get("cid")),
                    parseUuid(payload.get("bid"))
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INVALID_JWT);
        }
    }

    public UUID extractUserId(String token) {
        return validateToken(token).userId();
    }

    public String extractEmail(String token) {
        return validateToken(token).email();
    }

    public User.Role extractRole(String token) {
        return validateToken(token).role();
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }

    private String generateToken(User user, String tokenType, Instant issuedAt, Instant expiresAt) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", jwtConfig.issuer());
        payload.put("sub", user.getId().toString());
        payload.put("email", user.getEmail());
        payload.put("role", user.getRole().name());
        if (user.getCompanyId() != null) {
            payload.put("cid", user.getCompanyId().toString());
        }
        if (user.getBranchId() != null) {
            payload.put("bid", user.getBranchId().toString());
        }
        payload.put("typ", tokenType);
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());
        payload.put("jti", UUID.randomUUID().toString());

        String signingInput = encodeJson(header) + "." + encodeJson(payload);
        return signingInput + "." + sign(signingInput);
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(jwtConfig.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign JWT", ex);
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to encode JWT", ex);
        }
    }

    private Map<String, Object> readJson(String base64UrlJson) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(base64UrlJson);
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INVALID_JWT);
        }
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new BusinessException(ErrorCode.INVALID_JWT);
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        return UUID.fromString(value.toString());
    }

    public record JwtClaims(
            UUID userId,
            String email,
            User.Role role,
            String tokenType,
            Instant expiresAt,
            UUID companyId,
            UUID branchId
    ) {
    }
}
