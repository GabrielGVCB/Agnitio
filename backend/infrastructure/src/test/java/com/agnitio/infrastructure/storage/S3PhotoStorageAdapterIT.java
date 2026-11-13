package com.agnitio.infrastructure.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class S3PhotoStorageAdapterIT {

    private static final String ACCESS_KEY = "minio_admin";
    private static final String SECRET_KEY = "minio_secret_key";
    private static final String BUCKET = "agnitio-photos";

    @Container
    static GenericContainer<?> minio = new GenericContainer<>("bitnamilegacy/minio:latest")
            .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000))
            .withStartupTimeout(Duration.ofMinutes(2));

    private S3PhotoStorageAdapter adapter;

    @BeforeAll
    static void createBucket() {
        try (S3Client client = S3Client.builder()
                .endpointOverride(URI.create(endpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()) {
            client.createBucket(request -> request.bucket(BUCKET));
        }
    }

    @BeforeEach
    void setUp() {
        adapter = new S3PhotoStorageAdapter(endpoint(), "us-east-1", BUCKET, ACCESS_KEY, SECRET_KEY, true);
    }

    private static String endpoint() {
        return "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
    }

    @AfterEach
    void tearDown() {
        adapter.close();
    }

    @Test
    void shouldUploadThroughPresignedUrlAndDownloadSameBytes() throws Exception {
        String storageKey = "events/test/original/image.jpg";
        byte[] content = {1, 2, 3, 4};

        String uploadUrl = adapter.generatePresignedUploadUrl(storageKey, Duration.ofMinutes(15));

        HttpRequest putRequest = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Content-Type", "image/jpeg")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                .build();
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(putRequest, HttpResponse.BodyHandlers.ofString());
        }

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(adapter.download(storageKey)).containsExactly(content);
    }
}
