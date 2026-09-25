package spring4.tuto.group.service;

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
import spring4.tuto.group.domain.Group;
import spring4.tuto.group.dto.GroupDto;
import spring4.tuto.group.repository.GroupRepository;
import spring4.tuto.user.domain.User;
import spring4.tuto.user.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupDto createGroup(UUID ownerId, String name, String description, String avatarFileId,
                                 Long maxMembers, String joinPolicy, Boolean approvalRequired,
                                 List<UUID> initialMemberIds) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));

        Conversation conv = Conversation.builder()
                .type(ConversationType.GROUP)
                .title(name)
                .description(description)
                .avatarFileId(avatarFileId != null ? UUID.fromString(avatarFileId) : null)
                .owner(owner)
                .build();
        conv = conversationRepository.save(conv);

        Group group = Group.builder()
                .conversation(conv)
                .maxMembers(maxMembers != null ? maxMembers : 200L)
                .joinPolicy(joinPolicy != null ? joinPolicy : "EVERYONE")
                .approvalRequired(approvalRequired != null ? approvalRequired : false)
                .build();
        groupRepository.save(group);

        // Add owner as admin
        ConversationMember ownerMember = ConversationMember.builder()
                .id(new ConversationMember.ConversationMemberId(conv.getId(), ownerId))
                .role("ADMIN")
                .status("ACTIVE")
                .build();
        memberRepository.save(ownerMember);

        // Add initial members
        if (initialMemberIds != null) {
            for (UUID memberId : initialMemberIds) {
                if (!memberId.equals(ownerId)) {
                    User member = userRepository.findById(memberId)
                            .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));
                    ConversationMember m = ConversationMember.builder()
                            .id(new ConversationMember.ConversationMemberId(conv.getId(), memberId))
                            .role("MEMBER")
                            .status("ACTIVE")
                            .build();
                    memberRepository.save(m);
                }
            }
        }

        return GroupDto.fromEntity(conv, group);
    }

    @Transactional(readOnly = true)
    public GroupDto getGroup(UUID conversationId, UUID userId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        if (conv.getType() != ConversationType.GROUP) {
            throw new BadRequestException("Conversation is not a group");
        }

        if (!memberRepository.existsById_ConversationIdAndId_UserId(conversationId, userId)) {
            throw new BadRequestException("Access denied: Not a member of this group");
        }

        Group group = groupRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Group metadata not found"));

        return GroupDto.fromEntity(conv, group);
    }

    @Transactional
    public GroupDto updateGroup(UUID conversationId, UUID userId, String name, String description,
                                 String avatarFileId, Long maxMembers, String joinPolicy,
                                 Boolean approvalRequired) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        if (!isAdmin(conversationId, userId)) {
            throw new BadRequestException("Only admins can update group settings");
        }

        if (name != null) conv.setTitle(name);
        if (description != null) conv.setDescription(description);
        if (avatarFileId != null) conv.setAvatarFileId(UUID.fromString(avatarFileId));
        conversationRepository.save(conv);

        Group group = groupRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Group metadata not found"));

        if (maxMembers != null) group.setMaxMembers(maxMembers);
        if (joinPolicy != null) group.setJoinPolicy(joinPolicy);
        if (approvalRequired != null) group.setApprovalRequired(approvalRequired);
        groupRepository.save(group);

        return GroupDto.fromEntity(conv, group);
    }

    @Transactional
    public void deleteGroup(UUID conversationId, UUID userId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        if (!isOwner(conversationId, userId)) {
            throw new BadRequestException("Only the owner can delete the group");
        }

        conv.softDelete();
        conversationRepository.save(conv);
    }

    @Transactional
    public void addMember(UUID conversationId, UUID adminId, UUID newMemberId) {
        if (!isAdmin(conversationId, adminId)) {
            throw new BadRequestException("Only admins can add members");
        }

        Group group = groupRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        long currentMembers = memberRepository.countById_ConversationId(conversationId);
        if (currentMembers >= group.getMaxMembers()) {
            throw new BadRequestException("Group has reached maximum members");
        }

        User newMember = userRepository.findById(newMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (memberRepository.existsById_ConversationIdAndId_UserId(conversationId, newMemberId)) {
            throw new BadRequestException("User is already a member");
        }

        String status = "EVERYONE".equals(group.getJoinPolicy()) ||
                       (!group.getApprovalRequired() && "ADMINS_ONLY".equals(group.getJoinPolicy()))
                       ? "ACTIVE" : "PENDING";

        ConversationMember member = ConversationMember.builder()
                .id(new ConversationMember.ConversationMemberId(conversationId, newMemberId))
                .role("MEMBER")
                .status(status)
                .build();
        memberRepository.save(member);
    }

    @Transactional
    public void removeMember(UUID conversationId, UUID adminId, UUID targetUserId) {
        if (!isAdmin(conversationId, adminId) && !adminId.equals(targetUserId)) {
            throw new BadRequestException("Only admins can remove members (or leave yourself)");
        }

        if (isOwner(conversationId, targetUserId)) {
            throw new BadRequestException("Cannot remove group owner");
        }

        ConversationMember.ConversationMemberId memberId =
                new ConversationMember.ConversationMemberId(conversationId, targetUserId);
        memberRepository.deleteById(memberId);
    }

    @Transactional
    public void promoteToAdmin(UUID conversationId, UUID ownerId, UUID targetUserId) {
        if (!isOwner(conversationId, ownerId)) {
            throw new BadRequestException("Only the owner can promote to admin");
        }

        ConversationMember.ConversationMemberId memberId =
                new ConversationMember.ConversationMemberId(conversationId, targetUserId);
        ConversationMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        member.setRole("ADMIN");
        memberRepository.save(member);
    }

    @Transactional
    public void demoteFromAdmin(UUID conversationId, UUID ownerId, UUID targetUserId) {
        if (!isOwner(conversationId, ownerId)) {
            throw new BadRequestException("Only the owner can demote admins");
        }

        if (targetUserId.equals(ownerId)) {
            throw new BadRequestException("Cannot demote the owner");
        }

        ConversationMember.ConversationMemberId memberId =
                new ConversationMember.ConversationMemberId(conversationId, targetUserId);
        ConversationMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        member.setRole("MEMBER");
        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<GroupDto.MemberInfo> getMembers(UUID conversationId, UUID userId) {
        if (!memberRepository.existsById_ConversationIdAndId_UserId(conversationId, userId)) {
            throw new BadRequestException("Access denied");
        }

        return memberRepository.findById_ConversationId(conversationId).stream()
                .map(m -> {
                    User u = userRepository.findById(m.getId().getUserId()).orElse(null);
                    return GroupDto.MemberInfo.builder()
                            .userId(m.getId().getUserId())
                            .username(u != null ? u.getUsername() : null)
                            .displayName(u != null ? u.getFirstName() + " " + u.getLastName() : null)
                            .avatarFileId(u != null && u.getAvatarFileId() != null ? u.getAvatarFileId().toString() : null)
                            .role(m.getRole())
                            .status(m.getStatus())
                            .joinedAt(m.getJoinedAt())
                            .build();
                })
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