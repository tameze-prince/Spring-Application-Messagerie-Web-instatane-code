package spring4.tuto.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username may contain only letters, numbers, dots, underscores and hyphens")
    private String username;

    @NotBlank(message = "Email is required")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Email(message = "Email must be valid")
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    @Size(max = 80, message = "First name must not exceed 80 characters")
    @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "First name contains invalid characters")
    private String firstName;

    @Size(max = 80, message = "Last name must not exceed 80 characters")
    @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "Last name contains invalid characters")
    private String lastName;
}
