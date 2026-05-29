package lt.satsyuk.data.daos;

import lt.satsyuk.data.entities.core.RequestLog;
import org.springframework.stereotype.Repository;

@Repository
public interface IRequestLogRepository extends IRepository<RequestLog, Long> {
}
