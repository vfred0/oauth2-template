package lt.satsyuk.data.daos;

import lt.satsyuk.data.entities.core.rbac.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {}
