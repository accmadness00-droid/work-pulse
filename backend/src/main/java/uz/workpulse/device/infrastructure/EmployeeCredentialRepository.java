package uz.workpulse.device.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uz.workpulse.device.domain.EmployeeCredential;

public interface EmployeeCredentialRepository extends JpaRepository<EmployeeCredential, UUID>, JpaSpecificationExecutor<EmployeeCredential> {

    boolean existsByCredentialTypeAndExternalId(EmployeeCredential.CredentialType credentialType, String externalId);

    Optional<EmployeeCredential> findByCredentialTypeAndExternalIdAndActiveTrue(EmployeeCredential.CredentialType credentialType, String externalId);

    List<EmployeeCredential> findAllByEmployeeIdAndCredentialTypeAndActive(UUID employeeId, EmployeeCredential.CredentialType credentialType, boolean active);

    List<EmployeeCredential> findAllByEmployeeIdAndCredentialType(UUID employeeId, EmployeeCredential.CredentialType credentialType);

    List<EmployeeCredential> findAllByEmployeeIdAndActive(UUID employeeId, boolean active);

    List<EmployeeCredential> findAllByCredentialTypeAndActive(EmployeeCredential.CredentialType credentialType, boolean active);

    List<EmployeeCredential> findAllByEmployeeId(UUID employeeId);

    List<EmployeeCredential> findAllByCredentialType(EmployeeCredential.CredentialType credentialType);

    List<EmployeeCredential> findAllByActive(boolean active);
}
