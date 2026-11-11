package com.agnitio.infrastructure.storage;

import com.agnitio.application.ports.output.PhotoStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
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

    @org.springframework.beans.factory.annotation.Autowired
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

        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .serviceConfiguration(s3Configuration);

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        this.s3Presigner = builder.build();
    }

    public S3PhotoStorageAdapter(String bucket, S3Presigner s3Presigner) {
        this.bucket = Objects.requireNonNull(bucket, "bucket must not be null");
        this.s3Presigner = Objects.requireNonNull(s3Presigner, "s3Presigner must not be null");
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
    public void close() {
        if (s3Presigner != null) {
            s3Presigner.close();
        }
    }
}
