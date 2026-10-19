package com.vht.license.license;

import com.vht.license.common.PageDto;
import com.vht.license.license.LicenseService.Download;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @GetMapping
    public PageDto<LicenseDto> search(@RequestParam(required = false) Long marketId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return licenseService.search(marketId, page, size);
    }

    @GetMapping("/{id}")
    public LicenseDto get(@PathVariable Long id) {
        return licenseService.get(id);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Download d = licenseService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.fileName()).build().toString())
                .header("X-Checksum-SHA256", d.sha256())
                .body(d.content());
    }
}
