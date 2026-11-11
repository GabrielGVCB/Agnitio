package com.agnitio.infrastructure.client.vision;

import com.agnitio.application.dto.FaceMatchDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class VisionServiceHttpClientTest {

    private MockRestServiceServer mockServer;
    private VisionServiceHttpClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new VisionServiceHttpClient(builder, "http://localhost:8000", "test-key");
    }

    @Test
    @DisplayName("should search faces and parse matches from vision-service")
    void shouldSearchFacesAndParseMatches() {
        UUID eventId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();

        String jsonResponse = """
                {
                    "totalMatches": 1,
                    "matches": [
                        {
                            "photoId": "%s",
                            "similarity": 0.88
                        }
                    ]
                }
                """.formatted(photoId);

        mockServer.expect(requestTo("http://localhost:8000/api/v1/faces/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Api-Key", "test-key"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<FaceMatchDto> matches = client.searchFacesBySelfie(
                eventId,
                new byte[]{1, 2, 3},
                "selfie.jpg",
                0.6,
                50
        );

        mockServer.verify();
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).photoId()).isEqualTo(photoId);
        assertThat(matches.get(0).similarity()).isEqualTo(0.88);
    }
}
