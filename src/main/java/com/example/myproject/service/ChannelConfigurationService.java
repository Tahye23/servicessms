package com.example.myproject.service;

import com.example.myproject.domain.ChannelConfiguration;
import com.example.myproject.domain.enumeration.Channel;
import com.example.myproject.repository.ChannelConfigurationRepository;
import com.example.myproject.security.SecurityUtils;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ChannelConfigurationService {

    private final ChannelConfigurationRepository repo;
    private final TextEncryptor encryptor;

    public ChannelConfigurationService(ChannelConfigurationRepository repo, TextEncryptor encryptor) {
        this.repo = repo;
        this.encryptor = encryptor;
    }

    /**
     * Règles :
     * EMAIL → 1 seule config par user
     * SMS → 1 config par opérateur par user
     */
    public ChannelConfiguration saveOrUpdate(ChannelConfiguration cfg, String rawPassword) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié"));

        cfg.setUserLogin(login);

        if (cfg.getChannelType() == Channel.EMAIL) {
            Optional<ChannelConfiguration> existing = repo.findByUserLoginAndChannelType(login, Channel.EMAIL);

            if (existing.isPresent()) {
                if (cfg.getId() == null || !cfg.getId().equals(existing.get().getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une configuration EMAIL existe déjà pour cet utilisateur");
                }
            }
        } else if (cfg.getChannelType() == Channel.SMS) {
            if (cfg.getSmsOperator() == null || cfg.getSmsOperator().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'opérateur SMS est obligatoire");
            }

            Optional<ChannelConfiguration> existing = repo.findByUserLoginAndChannelTypeAndSmsOperator(
                login,
                Channel.SMS,
                cfg.getSmsOperator()
            );

            if (existing.isPresent()) {
                if (cfg.getId() == null || !cfg.getId().equals(existing.get().getId())) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Une configuration SMS existe déjà pour l'opérateur : " + cfg.getSmsOperator()
                    );
                }
            }
        }
        cfg.setVerified(true);
        // Toujours chiffrer
        cfg.setEncryptedPassword(encryptor.encrypt(rawPassword));

        return repo.save(cfg);
    }

    /**
     * EMAIL → retourne 1
     * SMS → retourne toutes les configs SMS
     */
    @Transactional(readOnly = true)
    public List<ChannelConfiguration> findByUserAndChannel(Channel channel) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié"));

        if (channel == Channel.EMAIL) {
            return repo.findByUserLoginAndChannelType(login, Channel.EMAIL).map(List::of).orElse(List.of());
        }

        return repo.findAllByUserLoginAndChannelType(login, Channel.SMS);
    }

    public String decryptPassword(ChannelConfiguration cfg) {
        return encryptor.decrypt(cfg.getEncryptedPassword());
    }

    public void delete(Long id) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié"));

        ChannelConfiguration cfg = repo
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuration introuvable"));

        if (!cfg.getUserLogin().equals(login)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        repo.delete(cfg);
    }

    public ChannelConfiguration update(Long id, ChannelConfiguration updatedCfg, String rawPassword) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié"));

        ChannelConfiguration existing = repo
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuration introuvable"));

        if (!existing.getUserLogin().equals(login)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        // Mise à jour des champs
        existing.setHost(updatedCfg.getHost());
        existing.setPort(updatedCfg.getPort());
        existing.setUsername(updatedCfg.getUsername());
        existing.setSmsOperator(updatedCfg.getSmsOperator());
        existing.setChannelType(updatedCfg.getChannelType());

        existing.setVerified(true);

        // Si password fourni → re-chiffrer
        if (rawPassword != null && !rawPassword.isBlank()) {
            existing.setEncryptedPassword(encryptor.encrypt(rawPassword));
        }

        return repo.save(existing);
    }

    public String encryptPassword(String rawPassword) {
        return encryptor.encrypt(rawPassword);
    }
}
