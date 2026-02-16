package com.example.myproject.service.mapper;

import com.example.myproject.domain.Configuration;
import com.example.myproject.service.dto.ConfigurationDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConfigurationMapper {
    ConfigurationDTO toDto(Configuration entity);
}
