package com.example.myproject.service.mapper;

import com.example.myproject.domain.PartnershipRequest;
import com.example.myproject.service.dto.PartnershipRequestDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PartnershipRequestMapper extends EntityMapper<PartnershipRequestDTO, PartnershipRequest> {
    @Mapping(target = "id", source = "id")
    PartnershipRequestDTO toDto(PartnershipRequest partnershipRequest);

    @Mapping(target = "id", source = "id")
    PartnershipRequest toEntity(PartnershipRequestDTO partnershipRequestDTO);

    default PartnershipRequest fromId(Long id) {
        if (id == null) {
            return null;
        }
        PartnershipRequest partnershipRequest = new PartnershipRequest();
        partnershipRequest.setId(id);
        return partnershipRequest;
    }
}

/**
 * Interface générique pour les mappers d'entités.
 */
interface EntityMapper<D, E> {
    E toEntity(D dto);

    D toDto(E entity);

    List<E> toEntity(List<D> dtoList);

    List<D> toDto(List<E> entityList);
}
