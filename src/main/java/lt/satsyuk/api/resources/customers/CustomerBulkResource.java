package lt.satsyuk.api.resources.customers;


import lt.satsyuk.api.dtos.customer.CustomerPatchDto;
import lt.satsyuk.api.dtos.customer.CustomerRequestDto;
import lt.satsyuk.api.resources.core.BulkResource;
import lt.satsyuk.config.api_version.ApiVersion;
import lt.satsyuk.service.core.operations.log.RequestLogService;
import lt.satsyuk.service.core.operations.route.ValidationRouteRegistry;
import lt.satsyuk.service.internal.customers.CustomerBulkService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/customers", version = ApiVersion.V1)
public class CustomerBulkResource extends BulkResource<CustomerRequestDto, CustomerPatchDto> {

    public CustomerBulkResource(CustomerBulkService service, RequestLogService requestLogService,
                                ValidationRouteRegistry registry) {
        super(service, requestLogService, registry, CustomerRequestDto.class, CustomerPatchDto.class);
    }
}
