package com.vht.license.user;

import com.vht.license.audit.AuditService;
import com.vht.license.common.BusinessException;
import com.vht.license.user.UserDtos.CreateUserRequest;
import com.vht.license.user.UserDtos.UpdateUserRequest;
import com.vht.license.user.UserDtos.UserDto;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final Clock clock;

    public UserService(AppUserRepository users, PasswordEncoder passwordEncoder, CurrentUser currentUser,
                       AuditService audit, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return users.findAll(Sort.by("username")).stream().map(UserDto::of).toList();
    }

    @Transactional
    public UserDto create(CreateUserRequest req) {
        if (users.existsByUsername(req.username())) {
            throw new BusinessException(HttpStatus.CONFLICT, "USERNAME_EXISTS", "Tên đăng nhập đã tồn tại");
        }
        AppUser user = users.save(new AppUser(req.username(), req.fullName(), req.email(), req.role(),
                passwordEncoder.encode(req.password()), clock.instant()));
        audit.log("USER_CREATED", "USER", user.getId(), "username=" + user.getUsername() + ", role=" + user.getRole());
        return UserDto.of(user);
    }

    @Transactional
    public UserDto update(Long id, UpdateUserRequest req) {
        AppUser user = users.findById(id).orElseThrow(() -> BusinessException.notFound("người dùng"));
        AppUser me = currentUser.get();
        if (me.getId().equals(user.getId()) && (!req.enabled() || req.role() != Role.ADMIN)) {
            throw BusinessException.badRequest("SELF_LOCKOUT", "Không thể tự khóa hoặc tự hạ quyền tài khoản của mình");
        }
        user.setFullName(req.fullName());
        user.setEmail(req.email());
        user.setRole(req.role());
        user.setEnabled(req.enabled());
        boolean passwordChanged = req.password() != null && !req.password().isBlank();
        if (passwordChanged) {
            user.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        audit.log("USER_UPDATED", "USER", user.getId(), "username=" + user.getUsername() + ", role=" + user.getRole()
                + ", enabled=" + user.isEnabled() + (passwordChanged ? ", passwordReset" : ""));
        return UserDto.of(user);
    }

    @Transactional
    public void changeOwnPassword(String oldPassword, String newPassword) {
        AppUser me = currentUser.get();
        if (!passwordEncoder.matches(oldPassword, me.getPasswordHash())) {
            throw BusinessException.badRequest("WRONG_PASSWORD", "Mật khẩu hiện tại không đúng");
        }
        me.setPasswordHash(passwordEncoder.encode(newPassword));
        audit.log("PASSWORD_CHANGED", "USER", me.getId(), null);
    }
}
