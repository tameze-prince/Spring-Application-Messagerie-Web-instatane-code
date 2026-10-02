package spring4.tuto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import spring4.tuto.auth.dto.AuthResponse;
import spring4.tuto.auth.dto.LoginRequest;
import spring4.tuto.auth.dto.RefreshTokenRequest;
import spring4.tuto.auth.dto.RegisterRequest;
import spring4.tuto.auth.security.JwtTokenProvider;
import spring4.tuto.auth.service.AuthService;
import spring4.tuto.common.exception.BadRequestException;
import spring4.tuto.notification.repository.NotificationPreferenceRepository;
import spring4.tuto.user.domain.User;
import spring4.tuto.user.domain.UserSession;
import spring4.tuto.user.repository.UserRepository;
import spring4.tuto.user.repository.UserSessionRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        registerRequest.setUsername("  TestUser  ");
        registerRequest.setEmail(" TEST@Example.COM ");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName(" Test ");
        registerRequest.setLastName(" User ");
    }

    @Test
    void register_Success_NormalizesIdentifiers() {
        UUID userId = UUID.randomUUID();
        User savedUser = User.builder()
                .username("TestUser")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .firstName("Test")
                .lastName("User")
                .build();
        savedUser.setId(userId);

        when(userRepository.existsByUsernameIgnoreCase("TestUser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh_token");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.register(registerRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertEquals("TestUser", captor.getValue().getUsername());
        assertEquals("test@example.com", captor.getValue().getEmail());
        assertEquals("Test", captor.getValue().getFirstName());
        assertEquals("User", captor.getValue().getLastName());
        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("TestUser", response.getUser().getUsername());
    }

    @Test
    void register_DuplicateUsername_ThrowsBadRequest() {
        when(userRepository.existsByUsernameIgnoreCase("TestUser")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_IsCaseInsensitiveAndRejectsDeletedUsers() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .username("TestUser")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .status("OFFLINE")
                .build();
        user.setId(userId);

        LoginRequest request = new LoginRequest();
        request.setLogin(" TEST@EXAMPLE.COM ");
        request.setPassword("password123");

        when(userRepository.findByEmailIgnoreCase("TEST@EXAMPLE.COM")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh_token");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.login(request);

        assertEquals("access_token", response.getAccessToken());
        assertEquals("ONLINE", user.getStatus());
        verify(sessionRepository).save(any(UserSession.class));

        user.softDelete();
        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    @Test
    void refresh_RejectsAccessToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("access-token");

        when(jwtTokenProvider.isRefreshToken("access-token")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.refreshToken(request));
        verifyNoInteractions(userRepository, sessionRepository);
    }

    @Test
    void refresh_RotatesSessionAndRefreshToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .username("TestUser")
                .email("test@example.com")
                .passwordHash("hash")
                .build();
        user.setId(userId);

        UserSession currentSession = UserSession.builder()
                .user(user)
                .refreshTokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        when(jwtTokenProvider.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("refresh-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(sessionRepository.findByRefreshTokenHash(anyString())).thenReturn(java.util.Optional.of(currentSession));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("new-refresh");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.refreshToken(request);

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertTrue(currentSession.isRevoked());
        verify(sessionRepository, times(2)).save(any(UserSession.class));
    }

    @Test
    void logoutAll_RevokesActiveSessions() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().username("test").email("test@example.com").passwordHash("hash").build();
        user.setId(userId);

        UserSession active1 = UserSession.builder().user(user).expiresAt(Instant.now().plusSeconds(600)).build();
        UserSession active2 = UserSession.builder().user(user).expiresAt(Instant.now().plusSeconds(600)).build();

        when(sessionRepository.findByUserIdAndRevokedAtIsNull(userId)).thenReturn(List.of(active1, active2));

        assertEquals(2, authService.logoutAll(userId));
        assertTrue(active1.isRevoked());
        assertTrue(active2.isRevoked());
        verify(sessionRepository).flush();
    }
}
