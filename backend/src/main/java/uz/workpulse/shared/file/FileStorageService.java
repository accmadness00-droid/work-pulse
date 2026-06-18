package uz.workpulse.shared.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final FileStorageProperties properties;

    public FileStorageService(FileStorageProperties properties) {
        this.properties = properties;
    }

    public StoredFile saveEmployeePhoto(UUID employeeId, MultipartFile file) {
        validateMultipartImage(file);
        String extension = getExtension(file.getOriginalFilename(), file.getContentType());
        String fileName = employeeId + "-" + Instant.now().toEpochMilli() + extension;
        try {
            return saveBytes("employees", fileName, file.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save employee photo", e);
        }
    }

    public StoredFile saveAttendancePhoto(UUID employeeId, String base64Image, String action) {
        byte[] bytes = decodeBase64Image(base64Image);
        if (bytes.length == 0 || bytes.length > properties.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException("Invalid image size");
        }
        String fileName = employeeId + "-" + action.toLowerCase() + "-" + Instant.now().toEpochMilli() + ".jpg";
        return saveBytes("attendance", fileName, bytes);
    }

    public byte[] decodeBase64Image(String base64Image) {
        if (!StringUtils.hasText(base64Image)) {
            throw new IllegalArgumentException("Photo is required");
        }
        String normalized = base64Image.trim();
        int comma = normalized.indexOf(',');
        if (normalized.startsWith("data:image") && comma > -1) {
            normalized = normalized.substring(comma + 1);
        }
        return Base64.getDecoder().decode(normalized);
    }

    private StoredFile saveBytes(String folder, String fileName, byte[] bytes) {
        try {
            Path dir = Path.of(properties.getUploadDir(), folder).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName).normalize();
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            String url = properties.getPublicBaseUrl().replaceAll("/$", "") + "/" + folder + "/" + fileName;
            return new StoredFile(fileName, target.toString(), url);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save file", e);
        }
    }

    private void validateMultipartImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photo is required");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException("Photo is too large");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG, and WEBP images are allowed");
        }
    }

    private String getExtension(String originalFilename, String contentType) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
            if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".webp")) {
                return ext.equals(".jpeg") ? ".jpg" : ext;
            }
        }
        if ("image/png".equals(contentType)) return ".png";
        if ("image/webp".equals(contentType)) return ".webp";
        return ".jpg";
    }
}
