package spring4.tuto.file.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
            @RequestBody UploadUrlRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        FileService.UploadUrlResponse response = fileService.requestUploadUrl(
                userId, request.getFilename(), request.getMimeType(), request.getSizeBytes()
        );
        return ResponseEntity.ok(ApiResponse.success("Presigned upload URL generated", response));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<FileDto>> completeUpload(
            @PathVariable UUID id,
            @RequestBody CompleteUploadRequest request) {
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
        @NotBlank
        private String filename;
        @NotBlank
        private String mimeType;
        @NotNull
        private Long sizeBytes;
    }

    @Data
    public static class CompleteUploadRequest {
        private String checksum;
    }
}
