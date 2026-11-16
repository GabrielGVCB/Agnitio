package com.agnitio.worker;

import com.agnitio.application.dto.EventProgress;
import com.agnitio.application.ports.output.PhotoProgressTracker;
import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.application.ports.output.PhotoStorage;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;
import com.agnitio.domain.photo.PhotoStatus;
import com.agnitio.domain.photo.PhotoUploadedEvent;
import com.agnitio.infrastructure.messaging.RabbitMqConfig;
import com.agnitio.infrastructure.persistence.event.EventJpaEntity;
import com.agnitio.infrastructure.persistence.event.SpringDataEventRepository;
import com.agnitio.infrastructure.persistence.photographer.PhotographerJpaEntity;
import com.agnitio.infrastructure.persistence.photographer.SpringDataPhotographerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class PhotoProcessingIntegrationTest {

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

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }

    @TestConfiguration
    static class TestStorageConfig {
        @Bean
        @Primary
        public TestPhotoStorage testPhotoStorage() {
            return new TestPhotoStorage();
        }
    }

    public static class TestPhotoStorage implements PhotoStorage {
        final Map<String, byte[]> storage = new ConcurrentHashMap<>();

        @Override
        public String generatePresignedUploadUrl(String storageKey, Duration duration) {
            return "http://localhost:9000/test/" + storageKey;
        }

        @Override
        public String generatePresignedDownloadUrl(String storageKey, Duration duration) {
            return "http://localhost:9000/test/" + storageKey;
        }

        @Override
        public byte[] download(String storageKey) {
            byte[] data = storage.get(storageKey);
            if (data == null) {
                throw new IllegalArgumentException("Not found: " + storageKey);
            }
            return data;
        }

        @Override
        public void upload(String storageKey, byte[] data, String contentType) {
            storage.put(storageKey, data);
        }
    }

    @Autowired
    private SpringDataPhotographerRepository photographerRepository;

    @Autowired
    private SpringDataEventRepository eventRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private PhotoProgressTracker progressTracker;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestPhotoStorage testPhotoStorage;

    private UUID photographerId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        photographerId = UUID.randomUUID();
        Instant now = Instant.now();
        photographerRepository.save(new PhotographerJpaEntity(
                photographerId,
                "worker-" + photographerId + "@test.com",
                "encoded_pw",
                "Worker Photographer",
                com.agnitio.domain.photographer.PhotographerRole.PHOTOGRAPHER,
                com.agnitio.domain.photographer.PhotographerStatus.ACTIVE,
                now,
                now
        ));

        eventId = UUID.randomUUID();
        eventRepository.save(new EventJpaEntity(
                eventId,
                photographerId,
                "Worker Test Race",
                com.agnitio.domain.event.EventType.RACE,
                com.agnitio.domain.event.EventVisibility.PUBLIC,
                null,
                30,
                com.agnitio.domain.event.EventStatus.ACTIVE,
                now,
                now,
                null,
                null
        ));
    }

    @Test
    @DisplayName("Should consume photo.uploaded, process image, save WebP variants, set READY and update Redis progress")
    void shouldConsumeAndProcessPhotoSuccessfully() throws Exception {
        // Arrange
        UUID photoId = UUID.randomUUID();
        Photo photo = new Photo(
                new PhotoId(photoId),
                new EventId(eventId),
                "runner.jpg",
                Instant.now(),
                PhotoStatus.UPLOADED
        );
        photoRepository.save(photo);

        // Put test JPEG into test storage
        byte[] testImageBytes = createTestJpegImage(800, 600);
        testPhotoStorage.upload(photo.getOriginalStorageKey(), testImageBytes, "image/jpeg");

        // Initialize progress for event
        progressTracker.initProgress(eventId, 1);

        // Publish photo.uploaded event
        PhotoUploadedEvent event = PhotoUploadedEvent.of(photo, UUID.randomUUID());
        String payload = objectMapper.writeValueAsString(event);

        // Act: publish to RabbitMQ exchange
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.PHOTOS_EXCHANGE,
                RabbitMqConfig.PHOTO_UPLOADED_ROUTING_KEY,
                payload
        );

        // Assert: wait for worker to complete processing
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Photo updatedPhoto = photoRepository.findById(new PhotoId(photoId)).orElse(null);
            assertThat(updatedPhoto).isNotNull();
            assertThat(updatedPhoto.getStatus()).isEqualTo(PhotoStatus.READY);

            assertThat(testPhotoStorage.storage).containsKey(photo.getThumbnailStorageKey());
            assertThat(testPhotoStorage.storage).containsKey(photo.getPreviewStorageKey());

            EventProgress progress = progressTracker.getProgress(eventId);
            assertThat(progress.total()).isEqualTo(1);
            assertThat(progress.processed()).isEqualTo(1);
            assertThat(progress.failed()).isEqualTo(0);
            assertThat(progress.isCompleted()).isTrue();
        });
    }

    private byte[] createTestJpegImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.GREEN);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Agnitio Worker Test", 50, 50);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }
}
