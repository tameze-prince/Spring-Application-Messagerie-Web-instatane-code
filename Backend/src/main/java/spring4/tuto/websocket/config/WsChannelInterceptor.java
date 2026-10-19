package spring4.tuto.websocket.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import spring4.tuto.auth.security.CustomUserDetailsService;
import spring4.tuto.auth.security.JwtTokenProvider;
import spring4.tuto.conversation.repository.ConversationMemberRepository;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WsChannelInterceptor implements ChannelInterceptor {

    private static final Pattern CONVERSATION_TOPIC =
            Pattern.compile("^/topic/conversations/([0-9a-fA-F-]{36})$");

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final ConversationMemberRepository memberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String token = extractAccessToken(accessor);

        if (!StringUtils.hasText(token) || !jwtTokenProvider.isAccessToken(token)) {
            throw new IllegalArgumentException("Authentication required");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserById(userId);

        accessor.setUser(new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        ));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        String destination = accessor.getDestination();
        if (!StringUtils.hasText(destination)) {
            throw new IllegalArgumentException("Subscription destination is required");
        }

        Matcher matcher = CONVERSATION_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        UUID conversationId = UUID.fromString(matcher.group(1));
        UUID userId = UUID.fromString(accessor.getUser().getName());

        if (!memberRepository.existsById_ConversationIdAndId_UserId(conversationId, userId)) {
            throw new IllegalArgumentException("Access denied to conversation");
        }
    }

    private String extractAccessToken(StompHeaderAccessor accessor) {
        List<String> authorization = accessor.getNativeHeader("Authorization");
        if (authorization != null && !authorization.isEmpty()) {
            String bearer = authorization.get(0);
            if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
                String token = bearer.substring(7).trim();
                if (StringUtils.hasText(token)) {
                    return token;
                }
            }
        }

        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            String token = tokenHeaders.get(0);
            return StringUtils.hasText(token) ? token.trim() : null;
        }

        return null;
    }
}
