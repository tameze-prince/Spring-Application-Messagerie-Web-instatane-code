package spring4.tuto.conversation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring4.tuto.common.exception.BadRequestException;
import spring4.tuto.common.exception.ResourceNotFoundException;
import spring4.tuto.conversation.domain.Conversation;
import spring4.tuto.conversation.domain.ConversationMember;
import spring4.tuto.conversation.domain.ConversationType;
import spring4.tuto.conversation.dto.ConversationDto;
import spring4.tuto.conversation.repository.ConversationMemberRepository;
import spring4.tuto.conversation.repository.ConversationRepository;
import spring4.tuto.message.domain.Message;
import spring4.tuto.message.repository.MessageRepository;
import spring4.tuto.user.domain.User;
import spring4.tuto.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public List<ConversationDto> getConversationsForUser(UUID userId) {
        List<Conversation> convs = conversationRepository.findConversationsByUserId(userId);
        return convs.stream().map(conv -> {
            ConversationDto dto = ConversationDto.fromEntity(conv);
            // Fetch last message preview
            Optional<Message> lastMsg = messageRepository.findFirstByConversationIdAndDeletedAtIsNullOrderBySequenceNumberDesc(conv.getId());
            if (lastMsg.isPresent()) {
                dto.setLastMessageBody(lastMsg.get().getBody());
                dto.setLastMessageAt(lastMsg.get().getCreatedAt());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public ConversationDto createOrGetPrivateConversation(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("Cannot create a private conversation with yourself");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        Optional<Conversation> existing = conversationRepository.findPrivateConversationBetweenUsers(
                ConversationType.PRIVATE, currentUserId, targetUserId
        );

        if (existing.isPresent()) {
            return ConversationDto.fromEntity(existing.get());
        }

        Conversation conv = Conversation.builder()
                .type(ConversationType.PRIVATE)
                .title(targetUser.getUsername())
                .owner(currentUser)
                .build();
        conv = conversationRepository.save(conv);

        ConversationMember m1 = ConversationMember.builder()
                .id(new ConversationMember.ConversationMemberId(conv.getId(), currentUserId))
                .role("MEMBER")
                .status("ACTIVE")
                .build();

        ConversationMember m2 = ConversationMember.builder()
                .id(new ConversationMember.ConversationMemberId(conv.getId(), targetUserId))
                .role("MEMBER")
                .status("ACTIVE")
                .build();

        memberRepository.save(m1);
        memberRepository.save(m2);

        return ConversationDto.fromEntity(conv);
    }

    @Transactional(readOnly = true)
    public ConversationDto getConversationById(UUID conversationId, UUID userId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!memberRepository.existsById_ConversationIdAndId_UserId(conversationId, userId)) {
            throw new BadRequestException("Access denied: Not a member of this conversation");
        }

        return ConversationDto.fromEntity(conv);
    }
}
