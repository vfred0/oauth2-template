package lt.satsyuk.repository;

import lt.satsyuk.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID> {
}

