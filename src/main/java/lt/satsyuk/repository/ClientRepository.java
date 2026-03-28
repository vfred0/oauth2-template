package lt.satsyuk.repository;

import lt.satsyuk.model.Client;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByPhone(String phone);

    List<Client> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrderByIdAsc(String firstNamePart,
                                                                                                 String lastNamePart,
                                                                                                 Pageable pageable);
}