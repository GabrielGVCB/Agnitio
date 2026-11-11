package com.agnitio.infrastructure.storage;

import com.agnitio.application.ports.output.PhotoStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

@Component
public class S3PhotoStorageAdapter implements PhotoStorage, AutoCloseable {

    private final String bucket;
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Autowired
    public S3PhotoStorageAdapter(
            @Value("${agnitio.storage.s3.endpoint:http://localhost:9000}") String endpoint,
            @Value("${agnitio.storage.s3.region:us-east-1}") String region,
            @Value("${agnitio.storage.s3.bucket:agnitio-photos}") String bucket,
            @Value("${agnitio.storage.s3.access-key:minio_admin}") String accessKey,
            @Value("${agnitio.storage.s3.secret-key:minio_secret_key}") String secretKey,
            @Value("${agnitio.storage.s3.path-style-access:true}") boolean pathStyleAccess
    ) {
        this.bucket = Objects.requireNonNull(bucket, "bucket must not be null");

        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccess)
                .build();

        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );

        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(s3Configuration);

        software.amazon.awssdk.services.s3.S3ClientBuilder clientBuilder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(s3Configuration);

        if (endpoint != null && !endpoint.isBlank()) {
            URI uri = URI.create(endpoint);
            presignerBuilder.endpointOverride(uri);
            clientBuilder.endpointOverride(uri);
        }

        this.s3Presigner = presignerBuilder.build();
        this.s3Client = clientBuilder.build();
    }

    public S3PhotoStorageAdapter(String bucket, S3Presigner s3Presigner, S3Client s3Client) {
        this.bucket = Objects.requireNonNull(bucket, "bucket must not be null");
        this.s3Presigner = Objects.requireNonNull(s3Presigner, "s3Presigner must not be null");
        this.s3Client = s3Client;
    }

    @Override
    public String generatePresignedUploadUrl(String storageKey, Duration duration) {
        Objects.requireNonNull(storageKey, "storageKey must not be null");
        Objects.requireNonNull(duration, "duration must not be null");

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(duration)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    @Override
    public String generatePresignedDownloadUrl(String storageKey, Duration duration) {
        Objects.requireNonNull(storageKey, "storageKey must not be null");
        Objects.requireNonNull(duration, "duration must not be null");

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    @Override
    public byte[] download(String storageKey) {
        Objects.requireNonNull(storageKey, "storageKey must not be null");
        if (s3Client == null) {
            throw new IllegalStateException("S3Client is not configured for downloads");
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();
        ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);
        return responseBytes.asByteArray();
    }

    @Override
    public void upload(String storageKey, byte[] data, String contentType) {
        Objects.requireNonNull(storageKey, "storageKey must not be null");
        Objects.requireNonNull(data, "data must not be null");
        if (s3Client == null) {
            throw new IllegalStateException("S3Client is not configured for uploads");
        }
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));
    }

    @Override
    public void close() {
        if (s3Presigner != null) {
            s3Presigner.close();
        }
        if (s3Client != null) {
            s3Client.close();
        }
    }
}
