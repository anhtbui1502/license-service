package com.vht.license.auth;

import com.vht.license.audit.AuditService;
import com.vht.license.user.CurrentUser;
import com.vht.license.user.UserDtos.UserDto;
import com.vht.license.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CurrentUser currentUser;
    private final UserService userService;
    private final AuditService audit;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository, CurrentUser currentUser,
                          UserService userService, AuditService audit) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.currentUser = currentUser;
        this.userService = userService;
        this.audit = audit;
    }

    public record LoginRequest(@NotBlank @Size(max = 100) String username, @NotBlank @Size(max = 100) String password) {
    }

    public record ChangePasswordRequest(@NotBlank String oldPassword, @NotBlank @Size(min = 8, max = 100) String newPassword) {
    }

    @PostMapping("/login")
    public UserDto login(@Valid @RequestBody LoginRequest req, HttpServletRequest request,
                         HttpServletResponse response) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(req.username(), req.password()));
        } catch (AuthenticationException ex) {
            audit.logIndependently(req.username(), "LOGIN_FAILED", null);
            throw ex;
        }
        // Chống session fixation
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        audit.logIndependently(auth.getName(), "LOGIN", null);
        return UserDto.of(currentUser.get());
    }

    @GetMapping("/me")
    public UserDto me() {
        return UserDto.of(currentUser.get());
    }

    @PostMapping("/change-password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        userService.changeOwnPassword(req.oldPassword(), req.newPassword());
    }
}
