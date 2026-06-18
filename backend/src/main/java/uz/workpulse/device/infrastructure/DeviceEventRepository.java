package uz.workpulse.device.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.workpulse.device.domain.DeviceEvent;

public interface DeviceEventRepository extends JpaRepository<DeviceEvent, UUID>, JpaSpecificationExecutor<DeviceEvent> {

    Optional<DeviceEvent> findByDeviceIdAndEventHash(UUID deviceId, String eventHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from DeviceEvent e where e.id = :id")
    Optional<DeviceEvent> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = """
            SELECT *
            FROM device_events
            WHERE processed = FALSE
              AND retry_count < :maxRetry
            ORDER BY created_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<DeviceEvent> findUnprocessedBatchForUpdate(@Param("batchSize") int batchSize, @Param("maxRetry") int maxRetry);
}
