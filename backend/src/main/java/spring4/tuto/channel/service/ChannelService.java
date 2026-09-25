package spring4.tuto.channel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring4.tuto.common.exception.BadRequestException;
import spring4.tuto.common.exception.ResourceNotFoundException;
import spring4.tuto.conversation.domain.Conversation;
import spring4.tuto.conversation.domain.ConversationMember;
import spring4.tuto.conversation.domain.ConversationType;
import spring4.tuto.conversation.repository.ConversationMemberRepository;
import spring4.tuto.conversation.repository.ConversationRepository;
import spring4.tuto.channel.domain.Channel;
import spring4.tuto.channel.dto.ChannelDto;
import spring4.tuto.channel.repository.ChannelRepository;
import spring4.tuto.user.domain.User;
import spring4.tuto.user.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChannelDto createChannel(UUID ownerId, String name, String username, String description,
                                     String avatarFileId, String visibility, String postPermission) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));

        if (channelRepository.existsByConversation_Username(username)) {
            throw new BadRequestException("Channel username already taken");
        }

        Conversation conv = Conversation.builder()
                .type(ConversationType.CHANNEL)
                .title(name)
                .username(username)
                .description(description)
                .avatarFileId(avatarFileId)
                .owner(owner)
                .build();
        conv = conversationRepository.save(conv);

        Channel channel = Channel.builder()
                .conversation(conv)
                .visibility(visibility != null ? visibility : "PUBLIC")
                .postPermission(postPermission != null ? postPermission : "ADMINS_ONLY")
                .subscriberCount(0L)
                .build();
        channelRepository.save(channel);

        // Add owner as admin
        ConversationMember ownerMember = ConversationMember.builder()
                .id(new ConversationMember.ConversationMemberId(conv.getId(), ownerId))
                .role("ADMIN")
                .status("ACTIVE")
                .build();
        memberRepository.save(ownerMember);

        return ChannelDto.fromEntity(conv, channel);
    }

    @Transactional(readOnly = true)
    public ChannelDto getChannel(UUID conversationId, UUID userId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found"));

        if (conv.getType() != ConversationType.CHANNEL) {
            throw new BadRequestException("Conversation is not a channel");
        }

        Channel channel = channelRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel metadata not found"));

        // Check if user is member or channel is public
        boolean isMember = memberRepository.existsById_ConversationIdAndId_UserId(conversationId, userId);
        if (!isMember && !"PUBLIC".equals(channel.getVisibility())) {
            throw new BadRequestException("Access denied: Channel is private");
        }

        return ChannelDto.fromEntity(conv, channel);
    }

    @Transactional(readOnly = true)
    public ChannelDto getChannelByUsername(String username, UUID userId) {
        Conversation conv = conversationRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found"));

        if (conv.getType() != ConversationType.CHANNEL) {
            throw new BadRequestException("Conversation is not a channel");
        }

        Channel channel = channelRepository.findByConversation_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("Channel metadata not found"));

        boolean isMember = memberRepository.existsById_ConversationIdAndId_UserId(conv.getId(), userId);
        if (!isMember && !"PUBLIC".equals(channel.getVisibility())) {
            throw new BadRequestException("Access denied: Channel is private");
        }

        return ChannelDto.fromEntity(conv, channel);
    }

    @Transactional
    public ChannelDto updateChannel(UUID conversationId, UUID userId, String name, String description,
                                     String avatarFileId, String username, String visibility,
                                     String postPermission) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found"));

        if (!isAdmin(conversationId, userId)) {
            throw new BadRequestException("Only admins can update channel settings");
        }

        if (username != null && !username.equals(conv.getUsername())) {
            if (channelRepository.existsByConversation_Username(username)) {
                throw new BadRequestException("Channel username already taken");
            }
            conv.setUsername(username);
        }

        if (name != null) conv.setTitle(name);
        if (description != null) conv.setDescription(description);
        if (avatarFileId != null) conv.setAvatarFileId(avatarFileId);
        conversationRepository.save(conv);

        Channel channel = channelRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel metadata not found"));

        if (visibility != null) channel.setVisibility(visibility);
        if (postPermission != null) channel.setPostPermission(postPermission);
        channelRepository.save(channel);

        return ChannelDto.fromEntity(conv, channel);
    }

    @Transactional
    public void deleteChannel(UUID conversationId, UUID userId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found"));

        if (!isOwner(conversationId, userId)) {
            throw new BadRequestException("Only the owner can delete the channel");
        }

        conv.softDelete();
        conversationRepository.save(conv);
    }

    @Transactional
    public void subscribe(UUID conversationId, UUID userId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found"));

        if (conv.getType() != ConversationType.CHANNEL) {
            throw new BadRequestException("Conversation is not a channel");
        }

        Channel channel = channelRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel metadata not found"));

        if (memberRepository.existsById_ConversationIdAndId_UserId(conversationId, userId)) {
            throw new BadRequestException("Already subscribed");
        }

        String status = "PUBLIC".equals(channel.getVisibility()) ? "ACTIVE" : "PENDING";

        ConversationMember member = ConversationMember.builder()
                .id(new ConversationMember.ConversationMemberId(conversationId, userId))
                .role("SUBSCRIBER")
                .status(status)
                .build();
        memberRepository.save(member);

        channel.setSubscriberCount(channel.getSubscriberCount() + 1);
        channelRepository.save(channel);
    }

    @Transactional
    public void unsubscribe(UUID conversationId, UUID userId) {
        if (!memberRepository.existsById_ConversationIdAndId_UserId(conversationId, userId)) {
            throw new BadRequestException("Not subscribed to this channel");
        }

        ConversationMember.ConversationMemberId memberId =
                new ConversationMember.ConversationMemberId(conversationId, userId);
        memberRepository.deleteById(memberId);

        Channel channel = channelRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel metadata not found"));

        channel.setSubscriberCount(Math.max(0, channel.getSubscriberCount() - 1));
        channelRepository.save(channel);
    }

    @Transactional(readOnly = true)
    public List<ChannelDto.SubscriberInfo> getSubscribers(UUID conversationId, UUID userId) {
        if (!memberRepository.existsById_ConversationIdAndId_UserId(conversationId, userId)) {
            throw new BadRequestException("Access denied");
        }

        return memberRepository.findById_ConversationId(conversationId).stream()
                .map(m -> {
                    User u = userRepository.findById(m.getId().getUserId()).orElse(null);
                    return ChannelDto.SubscriberInfo.builder()
                            .userId(m.getId().getUserId())
                            .username(u != null ? u.getUsername() : null)
                            .displayName(u != null ? u.getFirstName() + " " + u.getLastName() : null)
                            .avatarFileId(u != null ? u.getAvatarFileId() : null)
                            .role(m.getRole())
                            .status(m.getStatus())
                            .joinedAt(m.getJoinedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChannelDto> getPublicChannels() {
        return conversationRepository.findByType(ConversationType.CHANNEL).stream()
                .map(conv -> {
                    Channel channel = channelRepository.findById(conv.getId()).orElse(null);
                    return ChannelDto.fromEntity(conv, channel);
                })
                .filter(dto -> dto != null && "PUBLIC".equals(dto.getVisibility()))
                .collect(Collectors.toList());
    }

    private boolean isAdmin(UUID conversationId, UUID userId) {
        return memberRepository.findById(new ConversationMember.ConversationMemberId(conversationId, userId))
                .map(m -> "ADMIN".equals(m.getRole()) || "OWNER".equals(m.getRole()))
                .orElse(false);
    }

    private boolean isOwner(UUID conversationId, UUID userId) {
        return conversationRepository.findById(conversationId)
                .map(c -> c.getOwner() != null && c.getOwner().getId().equals(userId))
                .orElse(false);
    }
}