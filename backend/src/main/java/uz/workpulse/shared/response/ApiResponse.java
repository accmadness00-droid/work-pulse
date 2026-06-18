package uz.workpulse.shared.response;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        List<FieldErrorDetail> errors,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "OK", null, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(false, null, message, null, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String message, List<FieldErrorDetail> errors) {
        return new ApiResponse<>(false, null, message, errors, Instant.now());
    }
}
