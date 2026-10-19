package com.vht.license.market;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketRepository extends JpaRepository<Market, Long> {

    boolean existsByCode(String code);

    /** SELECT ... FOR UPDATE: tuần tự hóa mọi thao tác tạo yêu cầu / sinh license của cùng một thị trường. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Market m where m.id = :id")
    Optional<Market> lockById(@Param("id") Long id);
}
