package com.example.myproject.repository;

import com.example.myproject.domain.ChannelConfiguration;
import com.example.myproject.domain.enumeration.Channel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelConfigurationRepository extends JpaRepository<ChannelConfiguration, Long> {
    // EMAIL → 1 seule
    Optional<ChannelConfiguration> findByUserLoginAndChannelType(String userLogin, Channel channelType);

    Optional<ChannelConfiguration> findByUserLoginAndChannelTypeAndSmsOperator(String userLogin, Channel channelType, String smsOperator);

    List<ChannelConfiguration> findAllByUserLoginAndChannelType(String userLogin, Channel channelType);
}
