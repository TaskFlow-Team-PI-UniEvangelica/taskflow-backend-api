package unievangelica.taskflow.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizationInviteRequestDTO {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String role;
}
