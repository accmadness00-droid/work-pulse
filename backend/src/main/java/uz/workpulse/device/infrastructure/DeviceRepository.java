package uz.workpulse.device.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.workpulse.device.domain.Device;

public interface DeviceRepository extends JpaRepository<Device, UUID>, JpaSpecificationExecutor<Device> {

    Optional<Device> findBySerialNumber(String serialNumber);

    boolean existsBySerialNumber(String serialNumber);

    boolean existsBySerialNumberAndIdNot(String serialNumber, UUID id);

    List<Device> findAllByConnectionTypeAndStatus(Device.ConnectionType connectionType, Device.Status status);

    List<Device> findAllByBranchId(UUID branchId);

    List<Device> findAllByBranchIdAndTypeAndStatus(UUID branchId, Device.DeviceType type, Device.Status status);

    @Query("select d.id from Device d where d.branchId = :branchId")
    List<UUID> findIdsByBranchId(@Param("branchId") UUID branchId);

    @Query("select d.id from Device d where d.branchId in (select b.id from Branch b where b.companyId = :companyId)")
    List<UUID> findIdsByCompanyId(@Param("companyId") UUID companyId);
}
