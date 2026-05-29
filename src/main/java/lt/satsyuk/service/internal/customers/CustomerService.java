package lt.satsyuk.service.internal.customers;

import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import lt.satsyuk.api.dtos.customer.CustomerPatchDto;
import lt.satsyuk.api.dtos.customer.CustomerRequestDto;
import lt.satsyuk.api.http_errors.exceptions.InternalServerErrorException;
import lt.satsyuk.service.core.operations.route.ValidationRouteRegistry;
import lt.satsyuk.service.core.operations.single.SingleService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CustomerService extends SingleService<CustomerRequestDto, CustomerPatchDto> {

    protected CustomerService(Validator validator, ValidationRouteRegistry routeRegistry) {
        super(validator, routeRegistry);
    }

    @Override
    protected void onValidCreate(CustomerRequestDto item) {
        log.info("Processing processed dto (create): {}", item.getId());
        throw  new InternalServerErrorException("Simulated conflict error for dto " + item.getId());
    }

    @Override
    protected void onValidPatch(CustomerPatchDto item) {

    }
}
