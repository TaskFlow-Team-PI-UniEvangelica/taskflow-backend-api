package unievangelica.taskflow.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import unievangelica.taskflow.api.domain.service.OrganizationInviteService;
import unievangelica.taskflow.api.dto.request.OrganizationInviteRequestDTO;
import unievangelica.taskflow.api.dto.response.InviteResponseDTO;

import java.util.List;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
@Tag(name = "Organization Invites", description = "Endpoints for managing organization invites")
public class OrganizationInviteController {

    private final OrganizationInviteService inviteService;

    @PostMapping("/organization/{organizationId}/invites")
    @Operation(summary = "Invite a user to an organization")
    public ResponseEntity<Void> createInvite(
            @PathVariable Long organizationId,
            @Validated @RequestBody OrganizationInviteRequestDTO request) {
        inviteService.createInvite(organizationId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/me/invites")
    @Operation(summary = "Get pending invites for the current user")
    public ResponseEntity<List<InviteResponseDTO>> getMyInvites() {
        return ResponseEntity.ok(inviteService.getMyInvites());
    }

    @PostMapping("/user/me/invites/{inviteId}/accept")
    @Operation(summary = "Accept an organization invite")
    public ResponseEntity<Void> acceptInvite(@PathVariable Long inviteId) {
        inviteService.acceptInvite(inviteId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user/me/invites/{inviteId}/decline")
    @Operation(summary = "Decline an organization invite")
    public ResponseEntity<Void> declineInvite(@PathVariable Long inviteId) {
        inviteService.declineInvite(inviteId);
        return ResponseEntity.ok().build();
    }
}
