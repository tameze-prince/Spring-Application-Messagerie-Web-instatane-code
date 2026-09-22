package spring4.tuto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import spring4.tuto.auth.dto.AuthResponse;
import spring4.tuto.auth.dto.RegisterRequest;
import spring4.tuto.auth.security.JwtTokenProvider;
import spring4.tuto.auth.service.AuthService;
import spring4.tuto.common.exception.BadRequestException;
import spring4.tuto.notification.repository.NotificationPreferenceRepository;
import spring4.tuto.user.domain.User;
import spring4.tuto.user.repository.UserRepository;
import spring4.tuto.user.repository.UserSessionRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");
    }

    @Test
    void register_Success() {
        UUID userId = UUID.randomUUID();
        User savedUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .firstName("Test")
                .lastName("User")
                .build();
        savedUser.setId(userId);

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh_token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("testuser", response.getUser().getUsername());
    }

    @Test
    void register_DuplicateUsername_ThrowsBadRequest() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(registerRequest));
    }
}
