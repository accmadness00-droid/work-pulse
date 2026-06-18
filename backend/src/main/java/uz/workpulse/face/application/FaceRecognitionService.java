package uz.workpulse.face.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uz.workpulse.face.domain.EmployeeFaceProfile;
import uz.workpulse.face.domain.FaceEmbeddingResponse;
import uz.workpulse.face.domain.FaceMatchResult;
import uz.workpulse.face.infrastructure.EmployeeFaceProfileRepository;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;

@Service
@EnableConfigurationProperties(FaceProperties.class)
public class FaceRecognitionService {
    private final FaceServiceClient faceServiceClient;
    private final EmployeeFaceProfileRepository repository;
    private final ObjectMapper objectMapper;
    private final FaceProperties properties;

    public FaceRecognitionService(
            FaceServiceClient faceServiceClient,
            EmployeeFaceProfileRepository repository,
            ObjectMapper objectMapper,
            FaceProperties properties
    ) {
        this.faceServiceClient = faceServiceClient;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public FaceMatchResult recognize(byte[] imageBytes) {
        FaceEmbeddingResponse embeddingResponse = faceServiceClient.extractEmbedding(imageBytes);
        if (embeddingResponse == null || !embeddingResponse.isSuccess()) {
            throw faceError(embeddingResponse == null ? null : embeddingResponse.getError());
        }
        if (embeddingResponse.getEmbedding() == null || embeddingResponse.getEmbedding().isEmpty()) {
            throw new BusinessException(ErrorCode.FACE_NOT_DETECTED);
        }

        List<EmployeeFaceProfile> profiles = repository.findByActiveTrue();
        if (profiles.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPLOYEE_FACE_PROFILE_NOT_FOUND);
        }

        EmployeeFaceProfile bestProfile = null;
        double bestDistance = Double.MAX_VALUE;

        for (EmployeeFaceProfile profile : profiles) {
            List<Double> profileEmbedding = parseEmbedding(profile.getEmbeddingJson());
            double distance = cosineDistance(embeddingResponse.getEmbedding(), profileEmbedding);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestProfile = profile;
            }
        }

        if (bestProfile == null || bestDistance > properties.getThreshold()) {
            throw new BusinessException(ErrorCode.ATTENDANCE_FACE_NOT_RECOGNIZED);
        }

        return new FaceMatchResult(bestProfile.getEmployeeId(), bestProfile.getId(), bestDistance, bestProfile.getModelName());
    }

    private List<Double> parseEmbedding(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ATTENDANCE_FACE_NOT_RECOGNIZED);
        }
    }

    private BusinessException faceError(String error) {
        if (error != null) {
            if (error.contains("FACE_NOT_DETECTED")) {
                return new BusinessException(ErrorCode.FACE_NOT_DETECTED);
            }
            if (error.contains("FACE_MULTIPLE_DETECTED")) {
                return new BusinessException(ErrorCode.FACE_MULTIPLE_DETECTED);
            }
        }
        return new BusinessException(ErrorCode.FACE_SERVICE_UNAVAILABLE);
    }

    private double cosineDistance(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            throw new BusinessException(ErrorCode.ATTENDANCE_FACE_NOT_RECOGNIZED);
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.size(); i++) {
            double x = a.get(i);
            double y = b.get(i);
            dot += x * y;
            normA += x * x;
            normB += y * y;
        }
        if (normA == 0 || normB == 0) {
            return Double.MAX_VALUE;
        }
        return 1.0 - (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}
