package com.example.myproject.repository;

import com.example.myproject.domain.ChannelConfiguration;
import com.example.myproject.domain.enumeration.Channel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelConfigurationRepository extends JpaRepository<ChannelConfiguration, Long> {
    Optional<ChannelConfiguration> findByUserLoginAndChannelType(String userLogin, Channel channelType);
}
