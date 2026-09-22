package spring4.tuto.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring4.tuto.conversation.domain.Conversation;
import spring4.tuto.conversation.repository.ConversationMemberRepository;
import spring4.tuto.conversation.repository.ConversationRepository;
import spring4.tuto.message.domain.Message;
import spring4.tuto.message.dto.MessageDto;
import spring4.tuto.message.repository.MessageReactionRepository;
import spring4.tuto.message.repository.MessageRepository;
import spring4.tuto.message.service.MessageService;
import spring4.tuto.user.domain.User;
import spring4.tuto.user.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageReactionRepository reactionRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessageService messageService;

    @Test
    void sendMessage_Success() {
        UUID convId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        Conversation conv = Conversation.builder().build();
        conv.setId(convId);

        User sender = User.builder().username("senderUser").build();
        sender.setId(senderId);

        Message savedMsg = Message.builder()
                .conversation(conv)
                .sender(sender)
                .type("TEXT")
                .body("Hello World")
                .sequenceNumber(1L)
                .build();
        savedMsg.setId(UUID.randomUUID());

        when(memberRepository.existsById_ConversationIdAndId_UserId(convId, senderId)).thenReturn(true);
        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(messageRepository.findMaxSequenceNumberByConversationId(convId)).thenReturn(0L);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

        MessageDto result = messageService.sendMessage(convId, senderId, "TEXT", "Hello World", null);

        assertNotNull(result);
        assertEquals("Hello World", result.getBody());
        assertEquals("senderUser", result.getSenderUsername());
        assertEquals(1L, result.getSequenceNumber());
    }
}
