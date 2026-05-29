package lt.satsyuk.api.resources.customers;


import lt.satsyuk.api.dtos.customer.CustomerPatchDto;
import lt.satsyuk.api.dtos.customer.CustomerRequestDto;
import lt.satsyuk.api.resources.core.SingleResource;
import lt.satsyuk.config.api_version.ApiVersion;
import lt.satsyuk.service.core.operations.log.RequestLogService;
import lt.satsyuk.service.core.operations.route.ValidationRouteRegistry;
import lt.satsyuk.service.internal.customers.CustomerService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/customers", version = ApiVersion.V2)
public class CustomerSingleResource extends SingleResource<CustomerRequestDto, CustomerPatchDto> {

    public CustomerSingleResource(CustomerService service, RequestLogService requestLogService,
                                  ValidationRouteRegistry registry) {
        super(service, requestLogService, registry, CustomerRequestDto.class, CustomerPatchDto.class);
    }
}
