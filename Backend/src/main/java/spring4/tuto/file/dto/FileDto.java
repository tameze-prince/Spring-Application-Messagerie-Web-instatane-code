package spring4.tuto.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import spring4.tuto.file.domain.FileEntity;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileDto {
    private UUID id;
    private UUID ownerId;
    private String storageProvider;
    private String storageKey;
    private String originalName;
    private String mimeType;
    private Long sizeBytes;
    private String checksum;
    private String status;
    private Instant createdAt;

    public static FileDto fromEntity(FileEntity entity) {
        if (entity == null) return null;
        return FileDto.builder()
                .id(entity.getId())
                .ownerId(entity.getOwner() != null ? entity.getOwner().getId() : null)
                .storageProvider(entity.getStorageProvider())
                .storageKey(entity.getStorageKey())
                .originalName(entity.getOriginalName())
                .mimeType(entity.getMimeType())
                .sizeBytes(entity.getSizeBytes())
                .checksum(entity.getChecksum())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
