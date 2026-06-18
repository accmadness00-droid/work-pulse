package uz.workpulse.face.application;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uz.workpulse.face.domain.FaceEmbeddingResponse;

@Component
@EnableConfigurationProperties(FaceProperties.class)
public class FaceServiceClient {
    private final RestTemplate restTemplate;
    private final FaceProperties properties;

    public FaceServiceClient(FaceProperties properties) {
        this.restTemplate = new RestTemplate();
        this.properties = properties;
    }

    public FaceEmbeddingResponse extractEmbedding(byte[] imageBytes) {
        try {
            String url = properties.getServiceUrl().replaceAll("/$", "") + "/api/v1/face/embedding";

            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() { return "face.jpg"; }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", imageResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            return restTemplate.postForObject(url, request, FaceEmbeddingResponse.class);
        } catch (RestClientException e) {
            FaceEmbeddingResponse response = new FaceEmbeddingResponse();
            response.setSuccess(false);
            response.setError("FACE_SERVICE_UNAVAILABLE: " + e.getMessage());
            return response;
        }
    }
}
