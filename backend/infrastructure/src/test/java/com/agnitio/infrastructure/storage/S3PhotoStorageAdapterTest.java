package com.agnitio.infrastructure.storage;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class S3PhotoStorageAdapterTest {

    @Test
    void testUploadAndDownload() throws Exception {
        S3PhotoStorageAdapter adapter = new S3PhotoStorageAdapter(
                "http://localhost:9000",
                "us-east-1",
                "agnitio-photos",
                "minio_admin",
                "minio_secret_key",
                true
        );

        String uploadUrl = adapter.generatePresignedUploadUrl("test/image.jpg", Duration.ofMinutes(15));
        System.out.println("Generated presigned upload URL: " + uploadUrl);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest putRequest = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Content-Type", "image/jpeg")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(new byte[]{1, 2, 3, 4}))
                .build();

        HttpResponse<String> response = client.send(putRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("PUT response status: " + response.statusCode());
        System.out.println("PUT response body: " + response.body());

        assertThat(response.statusCode()).isEqualTo(200);

        byte[] downloaded = adapter.download("test/image.jpg");
        assertThat(downloaded).containsExactly(1, 2, 3, 4);

        adapter.close();
    }
}
