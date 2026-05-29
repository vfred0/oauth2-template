package lt.satsyuk.data.daos;

import lt.satsyuk.data.entities.core.rbac.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
