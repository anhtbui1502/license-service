package com.vht.license.license;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LicenseFileRepository extends JpaRepository<LicenseFile, Long>, JpaSpecificationExecutor<LicenseFile> {

    List<LicenseFile> findByCurrentTrue();

    Optional<LicenseFile> findByMarketIdAndCurrentTrue(Long marketId);

    Optional<LicenseFile> findByRequestId(Long requestId);

    @Query("select coalesce(max(l.version), 0) from LicenseFile l where l.market.id = :marketId")
    int maxVersion(@Param("marketId") Long marketId);
}
