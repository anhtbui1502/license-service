package com.vht.license.request;

import com.vht.license.common.PageDto;
import com.vht.license.license.LicenseDto;
import com.vht.license.request.RequestDtos.CreateRequest;
import com.vht.license.request.RequestDtos.RequestDto;
import com.vht.license.request.RequestDtos.VerifyRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
public class LicenseRequestController {

    private final LicenseRequestService service;

    public LicenseRequestController(LicenseRequestService service) {
        this.service = service;
    }

    @GetMapping
    public PageDto<RequestDto> search(@RequestParam(required = false) RequestStatus status,
                                      @RequestParam(required = false) Long marketId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return service.search(status, marketId, page, size);
    }

    @GetMapping("/{id}")
    public RequestDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public RequestDto create(@Valid @RequestBody CreateRequest req) {
        return service.create(req);
    }

    @PostMapping("/{id}/resend-otp")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public RequestDto resendOtp(@PathVariable Long id) {
        return service.resendOtp(id);
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public LicenseDto verify(@PathVariable Long id, @Valid @RequestBody VerifyRequest req) {
        return service.verifyAndGenerate(id, req.otp());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public RequestDto cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
