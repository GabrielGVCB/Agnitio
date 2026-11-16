package com.agnitio.api.controller;

import com.agnitio.api.controller.dto.CreateEventRequestDto;
import com.agnitio.api.controller.dto.LoginRequestDto;
import com.agnitio.api.controller.dto.RegisterRequestDto;
import com.agnitio.api.controller.dto.UpdateEventRequestDto;
import com.agnitio.domain.event.EventType;
import com.agnitio.domain.event.EventVisibility;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EventControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:8.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String email = "event-photographer-" + System.currentTimeMillis() + "@agnitio.com";

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequestDto(email, "Password123!", "Event Host"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequestDto(email, "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();

        token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asString();
    }

    @Test
    @DisplayName("Should create event, list events, update event and delete event")
    void shouldManageEventsLifecycle() throws Exception {
        // 1. Create Event
        CreateEventRequestDto createReq = new CreateEventRequestDto(
                "Rio Half Marathon",
                EventType.RACE,
                EventVisibility.PUBLIC,
                null,
                120,
                null,
                null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Rio Half Marathon"))
                .andExpect(jsonPath("$.type").value("RACE"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.biometricRetentionDays").value(120))
                .andReturn();

        String eventId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asString();

        // 2. List Events
        mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(eventId));

        // 3. Public access without token
        mockMvc.perform(get("/api/v1/events/" + eventId + "/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rio Half Marathon"));

        // 4. Update Event
        UpdateEventRequestDto updateReq = new UpdateEventRequestDto(
                "Rio Marathon Official 2026",
                EventType.RACE,
                EventVisibility.PRIVATE,
                "RIO2026",
                180,
                null,
                null
        );

        mockMvc.perform(put("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rio Marathon Official 2026"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.hasAccessCode").value(true));

        // 5. Public access now requires code
        mockMvc.perform(get("/api/v1/events/" + eventId + "/public"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/events/" + eventId + "/public?code=RIO2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rio Marathon Official 2026"));

        // 6. Delete Event
        mockMvc.perform(delete("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // 7. Verify Deleted
        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
    @Test
    @DisplayName("Should search public events anonymously without exposing private events or owner data")
    void shouldSearchPublicEventsAnonymously() throws Exception {
        String unique = "Busca" + System.nanoTime();

        CreateEventRequestDto publicEvent = new CreateEventRequestDto(
                unique + " Maratona", EventType.RACE, EventVisibility.PUBLIC, null, 30,
                LocalDate.of(2026, 5, 12), "Rio de Janeiro, RJ");
        CreateEventRequestDto privateEvent = new CreateEventRequestDto(
                unique + " Casamento", EventType.WEDDING, EventVisibility.PRIVATE, "SECRET", 30,
                LocalDate.of(2026, 6, 20), "Rio de Janeiro, RJ");

        for (CreateEventRequestDto request : new CreateEventRequestDto[]{publicEvent, privateEvent}) {
            mockMvc.perform(post("/api/v1/events")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.eventDate").value(request.eventDate().toString()))
                    .andExpect(jsonPath("$.city").value("Rio de Janeiro, RJ"));
        }

        // No Authorization header: participants have no account
        mockMvc.perform(get("/api/v1/events/public").param("query", unique.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value(unique + " Maratona"))
                .andExpect(jsonPath("$.items[0].eventDate").value("2026-05-12"))
                .andExpect(jsonPath("$.items[0].city").value("Rio de Janeiro, RJ"))
                .andExpect(jsonPath("$.items[0].photographerId").doesNotExist())
                .andExpect(jsonPath("$.items[0].hasAccessCode").doesNotExist())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("Should reject public search page size above the limit")
    void shouldRejectOversizedPublicSearchPage() throws Exception {
        mockMvc.perform(get("/api/v1/events/public").param("size", "500"))
                .andExpect(status().isBadRequest());
    }
}
