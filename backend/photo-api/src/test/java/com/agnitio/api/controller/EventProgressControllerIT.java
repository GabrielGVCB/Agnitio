package com.agnitio.api.controller;

import com.agnitio.application.ports.output.PhotoProgressTracker;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EventProgressControllerIT {

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
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PhotoProgressTracker progressTracker;

    @Test
    @DisplayName("Should return event progress status via JSON endpoint")
    void shouldReturnProgressStatus() throws Exception {
        UUID eventId = UUID.randomUUID();
        progressTracker.initProgress(eventId, 10);
        progressTracker.incrementProcessed(eventId);
        progressTracker.incrementProcessed(eventId);
        progressTracker.incrementFailed(eventId);

        mockMvc.perform(get("/api/v1/events/" + eventId + "/progress/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.processed").value(2))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.percentage").value(30.0));
    }

    @Test
    @DisplayName("Should initiate SSE stream for event progress")
    void shouldInitiateSseStream() throws Exception {
        UUID eventId = UUID.randomUUID();
        progressTracker.initProgress(eventId, 2);
        progressTracker.incrementProcessed(eventId);
        progressTracker.incrementProcessed(eventId);

        MvcResult result = mockMvc.perform(get("/api/v1/events/" + eventId + "/progress")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType()).contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
