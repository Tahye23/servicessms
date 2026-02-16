package com.example.myproject.service.mapper;

import com.example.myproject.domain.Abonnement;
import com.example.myproject.domain.ExtendedUser;
import com.example.myproject.domain.PlanAbonnement;
import com.example.myproject.service.dto.AbonnementDTO;
import com.example.myproject.service.dto.ExtendedUserDTO;
import com.example.myproject.service.dto.PlanabonnementDTO;
import java.util.List;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AbonnementMapper extends EntityMapper<AbonnementDTO, Abonnement> {
    // Conversion Abonnement -> AbonnementDTO
    @Named("toDto")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "planId", source = "plan.id")
    AbonnementDTO toDto(Abonnement abonnement);

    // Conversion AbonnementDTO -> Abonnement (mappe les objets complets)
    @Named("toEntity")
    @Mapping(target = "user", source = "user") // mappe ExtendedUserDTO -> ExtendedUser
    @Mapping(target = "plan", source = "plan") // mappe PlanAbonnementDTO -> PlanAbonnement
    Abonnement toEntity(AbonnementDTO abonnementDTO);

    // Conversion listes
    @IterableMapping(qualifiedByName = "toDto")
    List<AbonnementDTO> toDtoList(List<Abonnement> entities);

    @IterableMapping(qualifiedByName = "toEntity")
    List<Abonnement> toEntityList(List<AbonnementDTO> dtos);

    // Mappings spécifiques pour ExtendedUser et PlanAbonnement
    ExtendedUser toUserEntity(ExtendedUserDTO dto);
    ExtendedUserDTO toUserDto(ExtendedUser entity);

    PlanAbonnement toPlanEntity(PlanabonnementDTO dto);
    PlanabonnementDTO toPlanDto(PlanAbonnement entity);

    // Partial update (optionnel)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "plan", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    void partialUpdate(@MappingTarget Abonnement entity, AbonnementDTO dto);

    // Création et mise à jour (optionnel)
    @Named("toNewEntity")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "plan", source = "plan")
    Abonnement toNewEntity(AbonnementDTO dto);

    @Named("toUpdateEntity")
    @Mapping(target = "id", source = "id")
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "plan", source = "plan")
    Abonnement toUpdateEntity(AbonnementDTO dto);
}
