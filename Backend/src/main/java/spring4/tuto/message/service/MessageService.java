package spring4.tuto.message.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring4.tuto.common.exception.BadRequestException;
import spring4.tuto.common.exception.ResourceNotFoundException;
import spring4.tuto.conversation.domain.Conversation;
import spring4.tuto.conversation.repository.ConversationMemberRepository;
import spring4.tuto.conversation.repository.ConversationRepository;
import spring4.tuto.message.domain.Message;
import spring4.tuto.message.domain.MessageReaction;
import spring4.tuto.message.dto.MessageDto;
import spring4.tuto.message.repository.MessageReactionRepository;
import spring4.tuto.message.repository.MessageRepository;
import spring4.tuto.user.domain.User;
import spring4.tuto.user.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final MessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;

    private String validateMessageBody(String body) {
        if (body == null) {
            throw new BadRequestException("Message cannot be empty");
        }
        if (body.length() > MAX_MESSAGE_LENGTH) {
            throw new BadRequestException("Message must not exceed " + MAX_MESSAGE_LENGTH + " characters");
        }
        if (body.isBlank()) {
            throw new BadRequestException("Message cannot be empty");
        }
        return body.trim();
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getMessagesForConversation(UUID conversationId, UUID userId, int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page index must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }
        if (!memberRepository.existsById_ConversationIdAndId_UserId(conversationId, userId)) {
            throw new BadRequestException("Access denied to conversation messages");
        }

        Pageable pageable = PageRequest.of(page, size);
        List<Message> messages = messageRepository.findByConversationIdAndDeletedAtIsNullOrderBySequenceNumberDesc(conversationId, pageable);

        return messages.stream().map(msg -> {
            MessageDto dto = MessageDto.fromEntity(msg);
            List<String> reactions = reactionRepository.findByMessageId(msg.getId()).stream()
                    .map(MessageReaction::getReaction)
                    .collect(Collectors.toList());
            dto.setReactions(reactions);
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public MessageDto sendMessage(UUID conversationId, UUID senderId, String type, String body, UUID replyToMessageId) {
        String validatedBody = validateMessageBody(body);
        if (!memberRepository.existsById_ConversationIdAndId_UserId(conversationId, senderId)) {
            throw new BadRequestException("Sender is not a member of conversation");
        }

        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Message replyTo = null;
        if (replyToMessageId != null) {
            replyTo = messageRepository.findById(replyToMessageId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reply message not found"));
            if (replyTo.getConversation() == null || !conversationId.equals(replyTo.getConversation().getId())) {
                throw new BadRequestException("Reply message must belong to the same conversation");
            }
        }

        // Sequence number sequence per conversation
        Long maxSeq = messageRepository.findMaxSequenceNumberByConversationId(conversationId);
        Long newSeq = (maxSeq != null ? maxSeq : 0) + 1;

        Message message = Message.builder()
                .conversation(conv)
                .sender(sender)
                .type(type != null ? type : "TEXT")
                .body(validatedBody)
                .replyToMessage(replyTo)
                .sequenceNumber(newSeq)
                .build();

        message = messageRepository.save(message);
        return MessageDto.fromEntity(message);
    }

    @Transactional
    public MessageDto editMessage(UUID messageId, UUID userId, String newBody) {
        String validatedBody = validateMessageBody(newBody);
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!msg.getSender().getId().equals(userId)) {
            throw new BadRequestException("Cannot edit another user's message");
        }

        msg.setBody(validatedBody);
        msg.setEditedAt(Instant.now());
        msg = messageRepository.save(msg);
        return MessageDto.fromEntity(msg);
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!msg.getSender().getId().equals(userId)) {
            throw new BadRequestException("Cannot delete another user's message");
        }

        msg.softDelete();
        messageRepository.save(msg);
    }

    private String validateReaction(String reaction) {
        if (reaction == null || reaction.isBlank()) {
            throw new BadRequestException("Reaction cannot be empty");
        }
        String normalized = reaction.trim();
        if (normalized.length() > 32) {
            throw new BadRequestException("Reaction must not exceed 32 characters");
        }
        return normalized;
    }

    @Transactional
    public void addReaction(UUID messageId, UUID userId, String reactionStr) {
        String validatedReaction = validateReaction(reactionStr);
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<MessageReaction> existing = reactionRepository.findByMessageIdAndUserIdAndReaction(messageId, userId, validatedReaction);
        if (existing.isEmpty()) {
            MessageReaction reaction = MessageReaction.builder()
                    .message(msg)
                    .user(user)
                    .reaction(validatedReaction)
                    .build();
            reactionRepository.save(reaction);
        }
    }

    @Transactional
    public void removeReaction(UUID messageId, UUID userId, String reactionStr) {
        String validatedReaction = validateReaction(reactionStr);
        reactionRepository.deleteByMessageIdAndUserIdAndReaction(messageId, userId, validatedReaction);
    }
}
