package lt.satsyuk.api.resources;


import lt.satsyuk.api.dtos.customer.AddressDto;
import lt.satsyuk.api.resources.core.BulkResource;
import lt.satsyuk.config.api_version.ApiVersion;
import lt.satsyuk.service.core.operations.log.RequestLogService;
import lt.satsyuk.service.core.operations.route.ValidationRouteRegistry;
import lt.satsyuk.service.internal.address.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/address", version = ApiVersion.V1)
public class AddressBulkResource extends BulkResource<AddressDto, AddressDto> {

    public AddressBulkResource(AddressService service, RequestLogService requestLogService,
                               ValidationRouteRegistry registry) {
        super(service, requestLogService, registry, AddressDto.class, AddressDto.class);
    }

    @PostMapping(version = ApiVersion.V2)
    public ResponseEntity<?> create2(AddressDto dto) {
        return ResponseEntity.ok(dto);
    }
}