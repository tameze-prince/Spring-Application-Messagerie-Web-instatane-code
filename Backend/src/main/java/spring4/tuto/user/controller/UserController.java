package spring4.tuto.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring4.tuto.common.dto.ApiResponse;
import spring4.tuto.user.dto.UserDto;
import spring4.tuto.user.service.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        UserDto updated = userService.updateProfile(userId, request.getFirstName(), request.getLastName(), request.getBio(), request.getAvatarFileId());
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable UUID id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserDto>>> searchUsers(@RequestParam("q") String query) {
        List<UserDto> users = userService.searchUsers(query);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        userService.blockUser(userId, id);
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully", null));
    }

    @DeleteMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        userService.unblockUser(userId, id);
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", null));
    }

    @Data
    public static class UpdateProfileRequest {
        @Size(max = 80, message = "First name must not exceed 80 characters")
        @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "First name contains invalid characters")
        private String firstName;

        @Size(max = 80, message = "Last name must not exceed 80 characters")
        @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "Last name contains invalid characters")
        private String lastName;

        @Size(max = 500, message = "Bio must not exceed 500 characters")
        @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "Bio contains invalid characters")
        private String bio;

        private UUID avatarFileId;
    }
}
