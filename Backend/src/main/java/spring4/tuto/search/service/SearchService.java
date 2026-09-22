package spring4.tuto.search.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring4.tuto.conversation.dto.ConversationDto;
import spring4.tuto.conversation.repository.ConversationRepository;
import spring4.tuto.message.dto.MessageDto;
import spring4.tuto.message.repository.MessageRepository;
import spring4.tuto.user.dto.UserDto;
import spring4.tuto.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public GlobalSearchResult search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return GlobalSearchResult.builder()
                    .users(List.of())
                    .conversations(List.of())
                    .messages(List.of())
                    .build();
        }

        String trimmed = query.trim();

        List<UserDto> users = userRepository.searchUsersFts(trimmed).stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());

        return GlobalSearchResult.builder()
                .users(users)
                .conversations(List.of())
                .messages(List.of())
                .build();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GlobalSearchResult {
        private List<UserDto> users;
        private List<ConversationDto> conversations;
        private List<MessageDto> messages;
    }
}
