package spring4.tuto.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import spring4.tuto.user.domain.User;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String bio;
    private UUID avatarFileId;
    private String status;
    private Boolean emailVerified;
    private Instant lastSeenAt;
    private Instant createdAt;

    public static UserDto fromEntity(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .avatarFileId(user.getAvatarFileId())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .lastSeenAt(user.getLastSeenAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
