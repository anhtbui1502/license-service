package com.vht.license.crypto;

import com.vht.license.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

    private static final Logger log = LoggerFactory.getLogger(CryptoConfig.class);

    @Bean
    public LicenseCryptoProvider licenseCryptoProvider(AppProperties props) {
        String name = props.license().crypto();
        // Khi có thuật toán thật: thêm cài đặt LicenseCryptoProvider mới và nhánh tương ứng ở đây.
        if ("mock".equals(name)) {
            log.warn("ĐANG DÙNG LicenseCryptoProvider 'mock' — file license KHÔNG được mã hóa thật, chỉ dùng để thử nghiệm.");
            return new MockLicenseCryptoProvider();
        }
        throw new IllegalStateException("Không hỗ trợ app.license.crypto=" + name);
    }
}
