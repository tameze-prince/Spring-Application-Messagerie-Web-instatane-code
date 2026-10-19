package spring4.tuto.websocket.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import spring4.tuto.auth.security.CustomUserDetailsService;
import spring4.tuto.auth.security.JwtTokenProvider;
import spring4.tuto.conversation.repository.ConversationMemberRepository;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WsChannelInterceptorTest {

    private JwtTokenProvider jwtTokenProvider;
    private CustomUserDetailsService userDetailsService;
    private ConversationMemberRepository memberRepository;
    private WsChannelInterceptor interceptor;
    private UUID userId;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        memberRepository = mock(ConversationMemberRepository.class);
        interceptor = new WsChannelInterceptor(
                jwtTokenProvider,
                userDetailsService,
                memberRepository
        );
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
    }

    @Test
    void connect_withoutAccessToken_isRejected() {
        Message<?> message = stompMessage(StompCommand.CONNECT, null, null, null);

        assertThrows(
                IllegalArgumentException.class,
                () -> interceptor.preSend(message, mock(MessageChannel.class))
        );
    }

    @Test
    void connect_withRefreshToken_isRejected() {
        Message<?> message = stompMessage(
                StompCommand.CONNECT,
                null,
                "Bearer refresh-token",
                null
        );

        when(jwtTokenProvider.isAccessToken("refresh-token")).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> interceptor.preSend(message, mock(MessageChannel.class))
        );
    }

    @Test
    void connect_withValidAccessToken_setsPrincipal() {
        UserDetails details = new org.springframework.security.core.userdetails.User(
                userId.toString(),
                "hash",
                Collections.emptyList()
        );

        Message<?> message = stompMessage(
                StompCommand.CONNECT,
                null,
                "Bearer access-token",
                null
        );

        when(jwtTokenProvider.isAccessToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("access-token")).thenReturn(userId);
        when(userDetailsService.loadUserById(userId)).thenReturn(details);

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));
        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(result);

        assertNotNull(accessor.getUser());
        assertEquals(userId.toString(), accessor.getUser().getName());
    }

    @Test
    void subscribe_withoutMembership_isRejected() {
        var principal = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                Collections.emptyList()
        );

        Message<?> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/topic/conversations/" + conversationId,
                null,
                principal
        );

        when(memberRepository.existsById_ConversationIdAndId_UserId(conversationId, userId))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> interceptor.preSend(message, mock(MessageChannel.class))
        );
    }

    @Test
    void subscribe_withMembership_isAllowed() {
        var principal = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                Collections.emptyList()
        );

        Message<?> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/topic/conversations/" + conversationId,
                null,
                principal
        );

        when(memberRepository.existsById_ConversationIdAndId_UserId(conversationId, userId))
                .thenReturn(true);

        assertSame(
                message,
                interceptor.preSend(message, mock(MessageChannel.class))
        );
    }

    private Message<byte[]> stompMessage(
            StompCommand command,
            String destination,
            String authorization,
            UsernamePasswordAuthenticationToken principal
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);

        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        if (principal != null) {
            accessor.setUser(principal);
        }

        return MessageBuilder.createMessage(
                new byte[0],
                accessor.getMessageHeaders()
        );
    }
}
