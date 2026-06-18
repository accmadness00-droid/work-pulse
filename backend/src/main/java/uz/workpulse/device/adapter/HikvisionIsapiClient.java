package uz.workpulse.device.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uz.workpulse.device.adapter.dto.HikvisionEventPayload;
import uz.workpulse.device.domain.Device;

@Component
public class HikvisionIsapiClient {

    private static final DateTimeFormatter HIKVISION_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);
    private static final Pattern DIGEST_CHALLENGE = Pattern.compile("Digest\\s+(.*)", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean mockEnabled;
    private final Duration connectionTimeout;
    private final Duration readTimeout;

    public HikvisionIsapiClient(
            ObjectMapper objectMapper,
            @Value("${app.device.hikvision.mock-enabled:false}") boolean mockEnabled,
            @Value("${app.device.hikvision.connection-timeout-ms:5000}") long connectionTimeoutMs,
            @Value("${app.device.hikvision.read-timeout-ms:10000}") long readTimeoutMs
    ) {
        this.objectMapper = objectMapper;
        this.mockEnabled = mockEnabled;
        this.connectionTimeout = Duration.ofMillis(connectionTimeoutMs);
        this.readTimeout = Duration.ofMillis(readTimeoutMs);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectionTimeout)
                .build();
    }

    public String fetchAccessControlEventsJson(Device device, String username, String password, Instant since, Instant until) {
        if (mockEnabled || isMockDevice(device)) {
            return "{\"AcsEvent\":{\"numOfMatches\":0,\"InfoList\":[]}}";
        }
        String path = "/ISAPI/AccessControl/AcsEvent?format=json"
                + "&startTime=" + HIKVISION_TIME.format(since)
                + "&endTime=" + HIKVISION_TIME.format(until);
        return get(device, username, password, path);
    }

    public String fetchAlertStreamChunk(Device device, String username, String password, Duration maxWait) {
        if (mockEnabled || isMockDevice(device)) {
            return "";
        }
        String path = "/ISAPI/Event/notification/alertStream";
        return get(device, username, password, path, maxWait);
    }

    public HikvisionEventPayload parseAccessControlEvents(String json) {
        try {
            return objectMapper.readValue(json, HikvisionEventPayload.class);
        } catch (Exception ex) {
            return new HikvisionEventPayload(new HikvisionEventPayload.AcsEvent(List.of(), 0));
        }
    }

    public void uploadEmployeeFace(
            Device device,
            String username,
            String password,
            String employeeCode,
            String employeeName,
            byte[] imageBytes,
            String contentType
    ) {
        if (mockEnabled || isMockDevice(device)) {
            return;
        }
        String path = "/ISAPI/Intelligent/FDLib/FaceDataRecord?format=json";
        String metadata;
        try {
            metadata = objectMapper.writeValueAsString(new FaceDataRecord(
                    employeeCode,
                    employeeName,
                    "blackFD",
                    "1"
            ));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to build Hikvision face payload", ex);
        }
        postMultipart(device, username, password, path, metadata, imageBytes, contentType);
    }

    private String get(Device device, String username, String password, String path) {
        return get(device, username, password, path, readTimeout);
    }

    private String get(Device device, String username, String password, String path, Duration timeout) {
        URI uri = URI.create(baseUrl(device) + path);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 && StringUtils.hasText(username)) {
                String authorization = buildDigestAuthorization("GET", uri, username, password, response.headers().firstValue("WWW-Authenticate").orElse(""));
                HttpRequest authorized = HttpRequest.newBuilder(uri)
                        .timeout(timeout)
                        .header("Authorization", authorization)
                        .GET()
                        .build();
                response = httpClient.send(authorized, HttpResponse.BodyHandlers.ofString());
            }
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Hikvision ISAPI request failed with status " + response.statusCode());
            }
            return response.body();
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to call Hikvision ISAPI", ex);
        }
    }

    private void postMultipart(
            Device device,
            String username,
            String password,
            String path,
            String metadataJson,
            byte[] imageBytes,
            String contentType
    ) {
        URI uri = URI.create(baseUrl(device) + path);
        String boundary = "----workpulse-" + HexFormat.of().formatHex(randomBytes(12));
        byte[] body = multipartBody(boundary, metadataJson, imageBytes, contentType);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(readTimeout)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 && StringUtils.hasText(username)) {
                String authorization = buildDigestAuthorization("POST", uri, username, password, response.headers().firstValue("WWW-Authenticate").orElse(""));
                HttpRequest authorized = HttpRequest.newBuilder(uri)
                        .timeout(readTimeout)
                        .header("Authorization", authorization)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();
                response = httpClient.send(authorized, HttpResponse.BodyHandlers.ofString());
            }
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Hikvision ISAPI request failed with status " + response.statusCode() + ": " + response.body());
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to call Hikvision ISAPI", ex);
        }
    }

    private byte[] multipartBody(String boundary, String metadataJson, byte[] imageBytes, String contentType) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            writePartHeader(output, boundary, "FaceDataRecord", null, "application/json");
            output.write(metadataJson.getBytes(StandardCharsets.UTF_8));
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
            writePartHeader(output, boundary, "FaceImage", "face.jpg", StringUtils.hasText(contentType) ? contentType : "image/jpeg");
            output.write(imageBytes);
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
            output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to build multipart body", ex);
        }
    }

    private void writePartHeader(ByteArrayOutputStream output, String boundary, String name, String filename, String contentType) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        String disposition = "Content-Disposition: form-data; name=\"" + name + "\"";
        if (StringUtils.hasText(filename)) {
            disposition += "; filename=\"" + filename + "\"";
        }
        output.write((disposition + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private String buildDigestAuthorization(String method, URI uri, String username, String password, String challengeHeader) {
        Matcher matcher = DIGEST_CHALLENGE.matcher(challengeHeader.trim());
        if (!matcher.find()) {
            String basic = username + ":" + password;
            return "Basic " + Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8));
        }
        String challenge = matcher.group(1);
        String realm = extractChallengeValue(challenge, "realm");
        String nonce = extractChallengeValue(challenge, "nonce");
        String qop = extractChallengeValue(challenge, "qop");
        String opaque = extractChallengeValue(challenge, "opaque");
        String nc = "00000001";
        String cnonce = HexFormat.of().formatHex(randomBytes(8));
        String ha1 = md5(username + ":" + realm + ":" + password);
        String ha2 = md5(method + ":" + uri.getPath());
        String responseHash = StringUtils.hasText(qop)
                ? md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2)
                : md5(ha1 + ":" + nonce + ":" + ha2);

        StringBuilder authorization = new StringBuilder("Digest ");
        authorization.append("username=\"").append(username).append("\", ");
        authorization.append("realm=\"").append(realm).append("\", ");
        authorization.append("nonce=\"").append(nonce).append("\", ");
        authorization.append("uri=\"").append(uri.getPath()).append("\", ");
        authorization.append("response=\"").append(responseHash).append("\"");
        if (StringUtils.hasText(opaque)) {
            authorization.append(", opaque=\"").append(opaque).append("\"");
        }
        if (StringUtils.hasText(qop)) {
            authorization.append(", qop=").append(qop)
                    .append(", nc=").append(nc)
                    .append(", cnonce=\"").append(cnonce).append("\"");
        }
        return authorization.toString();
    }

    private String extractChallengeValue(String challenge, String key) {
        Pattern pattern = Pattern.compile(key + "=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(challenge);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format(Locale.ROOT, "%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute MD5", ex);
        }
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new java.security.SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private String baseUrl(Device device) {
        return "http://" + device.getIpAddress() + ":" + device.getPort();
    }

    private boolean isMockDevice(Device device) {
        return device.getIpAddress() != null && device.getIpAddress().startsWith("127.");
    }

    private record FaceDataRecord(
            String employeeNo,
            String name,
            String faceLibType,
            String FDID
    ) {
    }
}
