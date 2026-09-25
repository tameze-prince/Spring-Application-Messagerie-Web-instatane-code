package spring4.tuto.group.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import spring4.tuto.common.dto.ApiResponse;
import spring4.tuto.group.dto.GroupDto;
import spring4.tuto.group.service.GroupService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<ApiResponse<GroupDto>> createGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GroupDto.CreateGroupRequest request) {
        UUID ownerId = UUID.fromString(userDetails.getUsername());
        GroupDto group = groupService.createGroup(
                ownerId, request.getName(), request.getDescription(), request.getAvatarFileId(),
                request.getMaxMembers(), request.getJoinPolicy(), request.getApprovalRequired(),
                request.getInitialMemberIds());
        return ResponseEntity.ok(ApiResponse.success("Group created successfully", group));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupDto>> getGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        GroupDto group = groupService.getGroup(id, userId);
        return ResponseEntity.ok(ApiResponse.success(group));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupDto>> updateGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody GroupDto.UpdateGroupRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        GroupDto group = groupService.updateGroup(id, userId, request.getName(), request.getDescription(),
                request.getAvatarFileId(), request.getMaxMembers(), request.getJoinPolicy(),
                request.getApprovalRequired());
        return ResponseEntity.ok(ApiResponse.success("Group updated successfully", group));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        groupService.deleteGroup(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Group deleted successfully", null));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<GroupDto.MemberInfo>>> getMembers(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<GroupDto.MemberInfo> members = groupService.getMembers(id, userId);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody GroupDto.AddMemberRequest request) {
        UUID adminId = UUID.fromString(userDetails.getUsername());
        groupService.addMember(id, adminId, request.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Member added successfully", null));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        UUID adminId = UUID.fromString(userDetails.getUsername());
        groupService.removeMember(id, adminId, userId);
        return ResponseEntity.ok(ApiResponse.success("Member removed successfully", null));
    }

    @PostMapping("/{id}/admins/{userId}")
    public ResponseEntity<ApiResponse<Void>> promoteToAdmin(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        UUID ownerId = UUID.fromString(userDetails.getUsername());
        groupService.promoteToAdmin(id, ownerId, userId);
        return ResponseEntity.ok(ApiResponse.success("User promoted to admin", null));
    }

    @DeleteMapping("/{id}/admins/{userId}")
    public ResponseEntity<ApiResponse<Void>> demoteFromAdmin(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        UUID ownerId = UUID.fromString(userDetails.getUsername());
        groupService.demoteFromAdmin(id, ownerId, userId);
        return ResponseEntity.ok(ApiResponse.success("User demoted from admin", null));
    }
}