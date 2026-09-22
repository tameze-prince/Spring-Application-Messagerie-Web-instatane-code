package spring4.tuto.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

@Service
public class S3StorageService {

    private final S3Presigner presigner;
    private final String bucketName;

    public S3StorageService(
            @Value("${app.s3.endpoint:http://localhost:9000}") String endpoint,
            @Value("${app.s3.access-key:minioadmin}") String accessKey,
            @Value("${app.s3.secret-key:minioadmin}") String secretKey,
            @Value("${app.s3.bucket-name:tuto-uploads}") String bucketName,
            @Value("${app.s3.region:us-east-1}") String region) {

        this.bucketName = bucketName;
        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    public String generatePresignedUploadUrl(String objectKey, String contentType, Duration expiration) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .putObjectRequest(objectRequest)
                .build();

        return presigner.presignPutObject(presignRequest).url().toString();
    }

    public String getBucketName() {
        return bucketName;
    }
}
