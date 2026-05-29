package lt.satsyuk.data.daos;

import lt.satsyuk.data.entities.core.request.Request;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID> {
}

