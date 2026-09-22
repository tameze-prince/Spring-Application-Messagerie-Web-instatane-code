package spring4.tuto.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring4.tuto.common.exception.ResourceNotFoundException;
import spring4.tuto.user.domain.BlockedUser;
import spring4.tuto.user.domain.User;
import spring4.tuto.user.dto.UserDto;
import spring4.tuto.user.repository.BlockedUserRepository;
import spring4.tuto.user.repository.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BlockedUserRepository blockedUserRepository;

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserDto.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return userRepository.searchUsersFts(query.trim()).stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto updateProfile(UUID userId, String firstName, String lastName, String bio, UUID avatarFileId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (bio != null) user.setBio(bio);
        if (avatarFileId != null) user.setAvatarFileId(avatarFileId);

        user = userRepository.save(user);
        return UserDto.fromEntity(user);
    }

    @Transactional
    public void blockUser(UUID userId, UUID targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User to block not found"));

        BlockedUser.BlockedUserId id = new BlockedUser.BlockedUserId(userId, targetUserId);
        if (!blockedUserRepository.existsById(id)) {
            BlockedUser blockedUser = BlockedUser.builder()
                    .id(id)
                    .build();
            blockedUserRepository.save(blockedUser);
        }
    }

    @Transactional
    public void unblockUser(UUID userId, UUID targetUserId) {
        BlockedUser.BlockedUserId id = new BlockedUser.BlockedUserId(userId, targetUserId);
        if (blockedUserRepository.existsById(id)) {
            blockedUserRepository.deleteById(id);
        }
    }
}
