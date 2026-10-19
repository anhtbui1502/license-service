package com.vht.license.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RequestExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(RequestExpiryJob.class);

    private final LicenseRequestService service;

    public RequestExpiryJob(LicenseRequestService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.otp.expiry-job-interval:PT60S}", initialDelayString = "${app.otp.expiry-job-interval:PT60S}")
    public void run() {
        int n = service.expireOverdue();
        if (n > 0) {
            log.info("Đã đóng {} yêu cầu quá hạn OTP", n);
        }
    }
}
