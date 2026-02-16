package com.example.myproject.service;

import com.example.myproject.domain.ChannelConfiguration;
import com.example.myproject.repository.ChannelConfigurationRepository;
import com.example.myproject.security.SecurityUtils;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChannelConfigurationService {

    private final ChannelConfigurationRepository repo;
    private final TextEncryptor encryptor;

    public ChannelConfigurationService(ChannelConfigurationRepository repo, TextEncryptor encryptor) {
        this.repo = repo;
        this.encryptor = encryptor;
    }

    public ChannelConfiguration save(ChannelConfiguration cfg, String rawPassword) {
        cfg.setUserLogin(SecurityUtils.getCurrentUserLogin().orElseThrow());
        cfg.setEncryptedPassword(encryptor.encrypt(rawPassword));
        return repo.save(cfg);
    }

    public String decryptPassword(ChannelConfiguration cfg) {
        return encryptor.decrypt(cfg.getEncryptedPassword());
    }
}
