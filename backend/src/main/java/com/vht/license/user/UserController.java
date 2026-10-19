package com.vht.license.user;

import com.vht.license.user.UserDtos.CreateUserRequest;
import com.vht.license.user.UserDtos.UpdateUserRequest;
import com.vht.license.user.UserDtos.UserDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> list() {
        return userService.list();
    }

    @PostMapping
    public UserDto create(@Valid @RequestBody CreateUserRequest req) {
        return userService.create(req);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        return userService.update(id, req);
    }
}
