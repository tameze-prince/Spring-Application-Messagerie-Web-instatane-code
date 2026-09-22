package spring4.tuto.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring4.tuto.auth.dto.AuthResponse;
import spring4.tuto.auth.dto.LoginRequest;
import spring4.tuto.auth.dto.RefreshTokenRequest;
import spring4.tuto.auth.dto.RegisterRequest;
import spring4.tuto.auth.security.JwtTokenProvider;
import spring4.tuto.common.exception.BadRequestException;
import spring4.tuto.common.exception.ResourceNotFoundException;
import spring4.tuto.notification.domain.NotificationPreference;
import spring4.tuto.notification.repository.NotificationPreferenceRepository;
import spring4.tuto.user.domain.User;
import spring4.tuto.user.domain.UserSession;
import spring4.tuto.user.dto.UserDto;
import spring4.tuto.user.repository.UserRepository;
import spring4.tuto.user.repository.UserSessionRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status("OFFLINE")
                .emailVerified(false)
                .build();

        user = userRepository.save(user);

        // Initialize default notification preferences
        NotificationPreference pref = NotificationPreference.builder()
                .user(user)
                .messageNotifications(true)
                .groupNotifications(true)
                .channelNotifications(true)
                .mentionNotifications(true)
                .soundEnabled(true)
                .emailNotifications(false)
                .build();
        preferenceRepository.save(pref);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        saveSession(user, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(UserDto.fromEntity(user))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getLogin())
                .orElseGet(() -> userRepository.findByUsername(request.getLogin())
                        .orElseThrow(() -> new BadRequestException("Invalid email/username or password")));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email/username or password");
        }

        user.setStatus("ONLINE");
        user.setLastSeenAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        saveSession(user, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(UserDto.fromEntity(user))
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(request.getRefreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String hash = hashToken(request.getRefreshToken());
        UserSession session = sessionRepository.findByRefreshTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Session revoked or invalid"));

        if (session.isExpired() || session.isRevoked()) {
            throw new BadRequestException("Session expired or revoked");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        session.setRevokedAt(Instant.now());
        sessionRepository.save(session);

        saveSession(user, newRefreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .user(UserDto.fromEntity(user))
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        if (jwtTokenProvider.validateToken(refreshToken)) {
            String hash = hashToken(refreshToken);
            sessionRepository.findByRefreshTokenHash(hash).ifPresent(session -> {
                session.setRevokedAt(Instant.now());
                sessionRepository.save(session);
            });
        }
    }

    private void saveSession(User user, String refreshToken) {
        UserSession session = UserSession.builder()
                .user(user)
                .refreshTokenHash(hashToken(refreshToken))
                .deviceName("Web Client")
                .deviceType("WEB")
                .expiresAt(Instant.now().plusMillis(604800000L)) // 7 days
                .lastActiveAt(Instant.now())
                .build();
        sessionRepository.save(session);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}
