package com.vht.license.crypto;

/**
 * Điểm cắm thuật toán mã hóa file license của VHT.
 * Cài đặt thật cần trả về đúng định dạng mà service Open API ở thị trường giải mã được.
 */
public interface LicenseCryptoProvider {

    /** Tên cài đặt, khớp với cấu hình app.license.crypto. */
    String name();

    /** Đuôi file (không có dấu chấm), ví dụ "lic". */
    String fileExtension();

    byte[] encrypt(LicensePayload payload);

    LicensePayload decrypt(byte[] content);
}
