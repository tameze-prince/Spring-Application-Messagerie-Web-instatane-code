package spring4.tuto.file.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring4.tuto.common.exception.ResourceNotFoundException;
import spring4.tuto.file.domain.FileEntity;
import spring4.tuto.file.dto.FileDto;
import spring4.tuto.file.repository.FileRepository;
import spring4.tuto.user.domain.User;
import spring4.tuto.user.repository.UserRepository;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    @Transactional
    public UploadUrlResponse requestUploadUrl(UUID userId, String filename, String mimeType, Long sizeBytes) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String storageKey = "uploads/" + userId + "/" + UUID.randomUUID() + "_" + filename;

        FileEntity fileEntity = FileEntity.builder()
                .owner(owner)
                .storageProvider("S3")
                .storageKey(storageKey)
                .originalName(filename)
                .mimeType(mimeType)
                .sizeBytes(sizeBytes)
                .status("PENDING")
                .build();

        fileEntity = fileRepository.save(fileEntity);

        String uploadUrl = s3StorageService.generatePresignedUploadUrl(storageKey, mimeType, Duration.ofMinutes(15));

        return UploadUrlResponse.builder()
                .fileId(fileEntity.getId())
                .uploadUrl(uploadUrl)
                .storageKey(storageKey)
                .build();
    }

    @Transactional
    public FileDto completeUpload(UUID fileId, String checksum) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File record not found"));

        fileEntity.setChecksum(checksum);
        fileEntity.setStatus("COMPLETED");
        fileEntity = fileRepository.save(fileEntity);

        return FileDto.fromEntity(fileEntity);
    }

    @Transactional(readOnly = true)
    public FileDto getFileMetadata(UUID fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        return FileDto.fromEntity(fileEntity);
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UploadUrlResponse {
        private UUID fileId;
        private String uploadUrl;
        private String storageKey;
    }
}
