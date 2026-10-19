package com.vht.license.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserDto(Long id, String username, String fullName, String email, Role role, boolean enabled,
                          Instant createdAt) {
        public static UserDto of(AppUser u) {
            return new UserDto(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole(),
                    u.isEnabled(), u.getCreatedAt());
        }
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9._-]+", message = "chỉ gồm chữ, số, . _ -")
            String username,
            @Size(max = 200) String fullName,
            @NotBlank @Email @Size(max = 200) String email,
            @NotNull Role role,
            @NotBlank @Size(min = 8, max = 100) String password) {
    }

    public record UpdateUserRequest(
            @Size(max = 200) String fullName,
            @NotBlank @Email @Size(max = 200) String email,
            @NotNull Role role,
            boolean enabled,
            /** Để trống nếu không đổi mật khẩu. */
            @Size(min = 8, max = 100) String password) {
    }
}
