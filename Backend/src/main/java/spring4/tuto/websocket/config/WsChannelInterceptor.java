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

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WsChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authorization = accessor.getNativeHeader("Authorization");
            String token = null;

            if (authorization != null && !authorization.isEmpty()) {
                String bearerToken = authorization.get(0);
                if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                    token = bearerToken.substring(7);
                }
            }

            if (token == null) {
                List<String> tokenHeader = accessor.getNativeHeader("token");
                if (tokenHeader != null && !tokenHeader.isEmpty()) {
                    token = tokenHeader.get(0);
                }
            }

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                UUID userId = jwtTokenProvider.getUserIdFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                accessor.setUser(authentication);
            }
        }
        return message;
    }
}
