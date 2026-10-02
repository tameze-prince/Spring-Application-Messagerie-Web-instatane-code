package spring4.tuto.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spring4.tuto.auth.security.JwtTokenProvider;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String SECRET = "integration-test-jwt-secret-0123456789";
    private JwtTokenProvider jwtTokenProvider;
    private UUID userId;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, 60_000, 120_000);
        userId = UUID.randomUUID();
    }

    @Test
    void generateAccessToken_containsAccessTypeAndUserId() {
        String token = jwtTokenProvider.generateAccessToken(userId, "alice", "alice@example.com");

        assertTrue(jwtTokenProvider.validateToken(token));
        assertTrue(jwtTokenProvider.isAccessToken(token));
        assertEquals("ACCESS", jwtTokenProvider.getTokenType(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
    }

    @Test
    void generateRefreshToken_containsRefreshTypeAndUserId() {
        String token = jwtTokenProvider.generateRefreshToken(userId);

        assertTrue(jwtTokenProvider.validateToken(token));
        assertTrue(jwtTokenProvider.isRefreshToken(token));
        assertEquals("REFRESH", jwtTokenProvider.getTokenType(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
    }

    @Test
    void wrongTokenType_isRejected() {
        String accessToken = jwtTokenProvider.generateAccessToken(userId, "alice", "alice@example.com");
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        assertFalse(jwtTokenProvider.validateToken(accessToken, JwtTokenProvider.REFRESH_TOKEN_TYPE));
        assertFalse(jwtTokenProvider.validateToken(refreshToken, JwtTokenProvider.ACCESS_TOKEN_TYPE));
    }

    @Test
    void tamperedToken_isRejected() {
        String token = jwtTokenProvider.generateAccessToken(userId, "alice", "alice@example.com");
        String tampered = token.substring(0, token.length() - 1) + "x";

        assertFalse(jwtTokenProvider.validateToken(tampered));
    }

    @Test
    void shortSecret_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider("too-short", 60_000, 120_000));
    }
}
