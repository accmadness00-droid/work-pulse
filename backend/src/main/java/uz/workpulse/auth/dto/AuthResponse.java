package uz.workpulse.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
