package com.agnitio.infrastructure.client.vision;

import com.agnitio.application.dto.FaceMatchDto;
import com.agnitio.application.ports.output.FaceSearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class VisionServiceHttpClient implements FaceSearchClient {

    private static final Logger log = LoggerFactory.getLogger(VisionServiceHttpClient.class);

    private final RestClient restClient;
    private final String internalApiKey;

    @Autowired
    public VisionServiceHttpClient(
            @Value("${agnitio.vision.base-url:http://localhost:8000}") String baseUrl,
            @Value("${agnitio.vision.internal-api-key:dev-internal-key}") String internalApiKey
    ) {
        this(RestClient.builder().requestFactory(http11RequestFactory()), baseUrl, internalApiKey);
    }

    /**
     * Forces HTTP/1.1. By default the JDK HttpClient (RestClient's default in Spring Boot 4) sends
     * "Upgrade: h2c" with a chunked body; uvicorn does not support that upgrade and drops the body,
     * so the vision-service would receive an empty multipart request (422 "Field required").
     */
    static JdkClientHttpRequestFactory http11RequestFactory() {
        return new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
        );
    }

    public VisionServiceHttpClient(
            RestClient.Builder restClientBuilder,
            String baseUrl,
            String internalApiKey
    ) {
        this.restClient = (restClientBuilder != null ? restClientBuilder : RestClient.builder())
                .baseUrl(baseUrl)
                .build();
        this.internalApiKey = Objects.requireNonNull(internalApiKey, "internalApiKey must not be null");
    }

    @Override
    public List<FaceMatchDto> searchFacesBySelfie(
            UUID eventId,
            byte[] selfieBytes,
            String fileName,
            double threshold,
            int limit
    ) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(selfieBytes, "selfieBytes must not be null");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("eventId", eventId.toString());
        body.add("threshold", String.valueOf(threshold));
        body.add("limit", String.valueOf(limit));

        ByteArrayResource selfieResource = new ByteArrayResource(selfieBytes) {
            @Override
            public String getFilename() {
                return (fileName != null && !fileName.isBlank()) ? fileName : "selfie.jpg";
            }
        };
        body.add("selfie", selfieResource);

        try {
            VisionSearchResponse response = restClient.post()
                    .uri("/api/v1/faces/search")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(VisionSearchResponse.class);

            if (response == null || response.matches() == null) {
                return Collections.emptyList();
            }

            return response.matches().stream()
                    .map(m -> new FaceMatchDto(UUID.fromString(m.photoId()), m.similarity()))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to query vision-service face search for event {}: {}", eventId, e.getMessage());
            throw new RuntimeException("Vision service face search error: " + e.getMessage(), e);
        }
    }

    public record VisionSearchResponse(int totalMatches, List<VisionMatchDto> matches) {}

    public record VisionMatchDto(String photoId, double similarity) {}
}
