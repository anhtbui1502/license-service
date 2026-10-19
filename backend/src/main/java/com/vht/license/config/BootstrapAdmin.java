package com.vht.license.config;

import com.vht.license.user.AppUser;
import com.vht.license.user.AppUserRepository;
import com.vht.license.user.Role;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Tạo tài khoản ADMIN đầu tiên từ biến môi trường khi hệ thống chưa có người dùng nào. */
@Component
public class BootstrapAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;
    private final Clock clock;

    public BootstrapAdmin(AppUserRepository users, PasswordEncoder passwordEncoder, AppProperties props, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        AppProperties.Bootstrap b = props.bootstrap();
        if (b.adminUsername() == null || b.adminUsername().isBlank()
                || b.adminPassword() == null || b.adminPassword().length() < 8) {
            log.warn("Chưa có người dùng nào. Đặt BOOTSTRAP_ADMIN_USERNAME và BOOTSTRAP_ADMIN_PASSWORD (>= 8 ký tự) "
                    + "để tạo tài khoản ADMIN đầu tiên.");
            return;
        }
        users.save(new AppUser(b.adminUsername(), "Administrator", b.adminEmail(), Role.ADMIN,
                passwordEncoder.encode(b.adminPassword()), clock.instant()));
        log.info("Đã tạo tài khoản ADMIN đầu tiên: {}", b.adminUsername());
    }
}
