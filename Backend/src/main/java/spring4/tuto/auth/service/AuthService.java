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
import java.util.Locale;
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
        String username = normalizeUsername(request.getUsername());
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email already in use");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(normalizeOptional(request.getFirstName()))
                .lastName(normalizeOptional(request.getLastName()))
                .status("OFFLINE")
                .emailVerified(false)
                .build();

        user = userRepository.save(user);

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

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String login = request.getLogin() == null ? "" : request.getLogin().trim();

        User user = userRepository.findByEmailIgnoreCase(login)
                .orElseGet(() -> userRepository.findByUsernameIgnoreCase(login)
                        .orElseThrow(() -> new BadRequestException("Invalid email/username or password")));

        if (user.isDeleted() || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email/username or password");
        }

        user.setStatus("ONLINE");
        user.setLastSeenAt(Instant.now());
        userRepository.save(user);

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .filter(existing -> !existing.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String hash = hashToken(refreshToken);
        UserSession session = sessionRepository.findByRefreshTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Session revoked or invalid"));

        if (!user.getId().equals(session.getUser().getId())) {
            throw new BadRequestException("Session revoked or invalid");
        }

        if (session.isExpired() || session.isRevoked()) {
            throw new BadRequestException("Session expired or revoked");
        }

        session.setRevokedAt(Instant.now());
        sessionRepository.save(session);

        return createAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            return;
        }

        String hash = hashToken(refreshToken);
        sessionRepository.findByRefreshTokenHash(hash).ifPresent(session -> {
            if (!session.isRevoked()) {
                session.setRevokedAt(Instant.now());
                sessionRepository.save(session);
            }
        });
    }

    @Transactional
    public int logoutAll(UUID userId) {
        int revoked = 0;
        Instant now = Instant.now();

        for (UserSession session : sessionRepository.findByUserIdAndRevokedAtIsNull(userId)) {
            if (!session.isExpired()) {
                session.setRevokedAt(now);
                revoked++;
            }
        }

        sessionRepository.flush();
        return revoked;
    }

    private AuthResponse createAuthResponse(User user) {
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

    private void saveSession(User user, String refreshToken) {
        Instant now = Instant.now();
        UserSession session = UserSession.builder()
                .user(user)
                .refreshTokenHash(hashToken(refreshToken))
                .deviceName("Web Client")
                .deviceType("WEB")
                .expiresAt(now.plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .lastActiveAt(now)
                .build();
        sessionRepository.save(session);
    }

    private String normalizeUsername(String value) {
        return value.trim();
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null ? null : value.trim();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash refresh token", e);
        }
    }
}
