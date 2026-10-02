package spring4.tuto.auth.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtTokenProvider jwtTokenProvider;
    private CustomUserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;
    private UUID userId;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = Mockito.mock(JwtTokenProvider.class);
        userDetailsService = Mockito.mock(CustomUserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
        userId = UUID.randomUUID();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void queryParameterToken_isIgnoredForHttpRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("token", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        Mockito.verifyNoInteractions(jwtTokenProvider, userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void validAuthorizationBearer_accessTokenAuthenticatesUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        var details = new org.springframework.security.core.userdetails.User(
                userId.toString(), "hash", java.util.Collections.emptyList());

        when(jwtTokenProvider.isAccessToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("access-token")).thenReturn(userId);
        when(userDetailsService.loadUserById(userId)).thenReturn(details);

        filter.doFilter(request, response, new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userId.toString(), SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
