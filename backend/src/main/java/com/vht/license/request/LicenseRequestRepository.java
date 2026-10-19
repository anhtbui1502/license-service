package com.vht.license.request;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LicenseRequestRepository
        extends JpaRepository<LicenseRequest, Long>, JpaSpecificationExecutor<LicenseRequest> {

    Optional<LicenseRequest> findByPendingMarketId(Long marketId);

    List<LicenseRequest> findByStatus(RequestStatus status);

    @Query("select r.market.id from LicenseRequest r where r.id = :id")
    Optional<Long> findMarketIdById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LicenseRequest r where r.id = :id")
    Optional<LicenseRequest> lockById(@Param("id") Long id);

    /** Đóng hàng loạt các yêu cầu đã quá hạn OTP, giải phóng slot pending. */
    @Modifying
    @Query("""
            update LicenseRequest r
               set r.status = com.vht.license.request.RequestStatus.EXPIRED,
                   r.pendingMarketId = null, r.otpHash = null, r.closedAt = :now, r.closedBy = 'system'
             where r.status = com.vht.license.request.RequestStatus.PENDING_OTP
               and r.otpExpiresAt <= :now""")
    int expireOverdue(@Param("now") Instant now);
}
