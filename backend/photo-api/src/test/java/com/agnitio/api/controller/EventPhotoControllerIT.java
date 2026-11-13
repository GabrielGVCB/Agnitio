package com.agnitio.api.controller;

import com.agnitio.api.controller.dto.ConfirmUploadRequestDto;
import com.agnitio.api.controller.dto.CreateEventRequestDto;
import com.agnitio.api.controller.dto.LoginRequestDto;
import com.agnitio.api.controller.dto.RegisterRequestDto;
import com.agnitio.api.controller.dto.RequestUploadUrlsRequestDto;
import com.agnitio.domain.event.EventType;
import com.agnitio.domain.event.EventVisibility;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EventPhotoControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:8.2-alpine")
            .withExposedPorts(6379);

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:4.4-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
        registry.add("agnitio.storage.s3.endpoint", () -> "http://localhost:9000");
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private String registerAndLogin(String email, String name) throws Exception {
        RegisterRequestDto reg = new RegisterRequestDto(email, "StrongP@ssw0rd123", name);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequestDto login = new LoginRequestDto(email, "StrongP@ssw0rd123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("accessToken").asText();
    }

    private String createEvent(String token, String name) throws Exception {
        CreateEventRequestDto req = new CreateEventRequestDto(
                name,
                EventType.RACE,
                EventVisibility.PUBLIC,
                null,
                30
        );

        MvcResult result = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("id").asText();
    }

    @Test
    @DisplayName("Should request upload URLs, confirm upload, and retrieve event gallery")
    void shouldSuccessfullyManagePhotoUploadLifecycle() throws Exception {
        String token = registerAndLogin("uploader@example.com", "Uploader Test");
        String eventId = createEvent(token, "Marathon Rio 2026");

        // 1. Request presigned upload URLs
        RequestUploadUrlsRequestDto uploadReq = new RequestUploadUrlsRequestDto(List.of(
                new RequestUploadUrlsRequestDto.UploadItemDto("pic1.jpg"),
                new RequestUploadUrlsRequestDto.UploadItemDto("pic2.png")
        ));

        MvcResult uploadUrlsResult = mockMvc.perform(post("/api/v1/events/" + eventId + "/photos/upload-urls")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uploadReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].uploadUrl").isNotEmpty())
                .andReturn();

        JsonNode uploadNode = objectMapper.readTree(uploadUrlsResult.getResponse().getContentAsString());
        UUID photoId1 = UUID.fromString(uploadNode.get("items").get(0).get("photoId").asText());
        UUID photoId2 = UUID.fromString(uploadNode.get("items").get(1).get("photoId").asText());

        // 2. Confirm upload
        ConfirmUploadRequestDto confirmReq = new ConfirmUploadRequestDto(List.of(photoId1, photoId2));
        mockMvc.perform(post("/api/v1/events/" + eventId + "/photos/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedCount").value(2))
                .andExpect(jsonPath("$.confirmedPhotoIds").isArray());

        // 3. Retrieve public gallery (no auth required for public event)
        mockMvc.perform(get("/api/v1/events/" + eventId + "/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.totalPhotos").value(2))
                .andExpect(jsonPath("$.photos[0].status").value("UPLOADED"));
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user requests upload URLs")
    void shouldRejectUnauthenticatedUploadRequest() throws Exception {
        RequestUploadUrlsRequestDto uploadReq = new RequestUploadUrlsRequestDto(List.of(
                new RequestUploadUrlsRequestDto.UploadItemDto("pic.jpg")
        ));

        mockMvc.perform(post("/api/v1/events/" + UUID.randomUUID() + "/photos/upload-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uploadReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 403 when photographer tries to upload photos to another photographer's event")
    void shouldForbidUploadToOtherPhotographerEvent() throws Exception {
        String token1 = registerAndLogin("owner@example.com", "Owner");
        String token2 = registerAndLogin("intruder@example.com", "Intruder");

        String eventId = createEvent(token1, "Owner Marathon");

        RequestUploadUrlsRequestDto uploadReq = new RequestUploadUrlsRequestDto(List.of(
                new RequestUploadUrlsRequestDto.UploadItemDto("pic.jpg")
        ));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/photos/upload-urls")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uploadReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 400 when biometric consent is not given")
    void shouldRejectSelfieSearchWithoutConsent() throws Exception {
        UUID eventId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "selfie",
                "selfie.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/events/" + eventId + "/photos/search-by-selfie")
                        .file(file)
                        .param("consent", "false"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BIOMETRIC_CONSENT_REQUIRED"));
    }
}
