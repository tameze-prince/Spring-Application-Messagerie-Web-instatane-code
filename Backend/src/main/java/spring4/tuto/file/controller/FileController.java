package spring4.tuto.file.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring4.tuto.common.dto.ApiResponse;
import spring4.tuto.file.dto.FileDto;
import spring4.tuto.file.service.FileService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload-url")
    public ResponseEntity<ApiResponse<FileService.UploadUrlResponse>> requestUploadUrl(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UploadUrlRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        FileService.UploadUrlResponse response = fileService.requestUploadUrl(
                userId, request.getFilename(), request.getMimeType(), request.getSizeBytes()
        );
        return ResponseEntity.ok(ApiResponse.success("Presigned upload URL generated", response));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<FileDto>> completeUpload(
            @PathVariable UUID id,
            @Valid @RequestBody CompleteUploadRequest request) {
        FileDto fileDto = fileService.completeUpload(id, request.getChecksum());
        return ResponseEntity.ok(ApiResponse.success("File upload marked as completed", fileDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileDto>> getFileMetadata(@PathVariable UUID id) {
        FileDto fileDto = fileService.getFileMetadata(id);
        return ResponseEntity.ok(ApiResponse.success(fileDto));
    }

    @Data
    public static class UploadUrlRequest {
        @NotBlank(message = "Filename is required")
        @Size(max = 255, message = "Filename must not exceed 255 characters")
        private String filename;

        @NotBlank(message = "MIME type is required")
        @Size(max = 127, message = "MIME type must not exceed 127 characters")
        private String mimeType;

        @NotNull(message = "File size is required")
        @Positive(message = "File size must be positive")
        @Max(value = 52428800, message = "File size must not exceed 50 MiB")
        private Long sizeBytes;
    }

    @Data
    public static class CompleteUploadRequest {
        @NotBlank(message = "Checksum is required")
        @Size(max = 255, message = "Checksum must not exceed 255 characters")
        private String checksum;
    }
}
