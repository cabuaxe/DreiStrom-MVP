package de.dreistrom.document.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3StorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${dreistrom.document.s3.bucket:dreistrom-documents}") String bucketName) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    /**
     * Upload file to S3 with AES-256 SSE.
     * Returns the S3 object key.
     */
    public String upload(Long userId, String fileName, String contentType,
                         byte[] content) {
        String key = buildKey(userId, fileName);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .serverSideEncryption(ServerSideEncryption.AES256)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));
        return key;
    }

    /**
     * Generate a pre-signed download URL valid for 15 minutes.
     */
    public URL getPresignedDownloadUrl(String s3Key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(b -> b.bucket(bucketName).key(s3Key))
                .build();

        return s3Presigner.presignGetObject(presignRequest).url();
    }

    /**
     * Download file content as InputStream.
     */
    public InputStream download(String s3Key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        return s3Client.getObject(request);
    }

    /**
     * Delete object from S3.
     */
    public void delete(String s3Key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.deleteObject(request);
    }

    private String buildKey(Long userId, String fileName) {
        return "documents/%d/%s/%s".formatted(userId, UUID.randomUUID(), fileName);
    }
}
