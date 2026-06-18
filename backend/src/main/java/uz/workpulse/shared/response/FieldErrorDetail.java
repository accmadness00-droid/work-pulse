package uz.workpulse.shared.response;

public record FieldErrorDetail(
        String field,
        String message
) {
}
