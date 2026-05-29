package lt.satsyuk.config.mapper;

import lt.satsyuk.api.dtos.client.ClientResponse;
import lt.satsyuk.api.dtos.client.CreateClientRequest;
import lt.satsyuk.data.entities.core.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    ClientMapper INSTANCE = Mappers.getMapper(ClientMapper.class);

    @Mapping(target = "id", ignore = true)
    Client toEntity(CreateClientRequest request);

    ClientResponse toResponse(Client client);
}