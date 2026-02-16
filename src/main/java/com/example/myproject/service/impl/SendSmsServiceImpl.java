package com.example.myproject.service.impl;

import com.example.myproject.domain.SendSms;
import com.example.myproject.domain.enumeration.MessageType;
import com.example.myproject.repository.SendSmsRepository;
import com.example.myproject.service.SendSmsService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.example.myproject.domain.SendSms}.
 */
@Service
@Transactional
public class SendSmsServiceImpl implements SendSmsService {

    private final Logger log = LoggerFactory.getLogger(SendSmsServiceImpl.class);

    private final SendSmsRepository sendSmsRepository;

    public SendSmsServiceImpl(SendSmsRepository sendSmsRepository) {
        this.sendSmsRepository = sendSmsRepository;
    }

    @Override
    public SendSms save(SendSms sendSms) {
        log.debug("Request to save SendSms : {}", sendSms);
        return sendSmsRepository.save(sendSms);
    }

    @Override
    public SendSms update(SendSms sendSms) {
        log.debug("Request to update SendSms : {}", sendSms);
        return sendSmsRepository.save(sendSms);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SendSms> findFiltered(
        String search,
        Boolean isSent,
        Boolean isBulk,
        String receiver,
        String receivers,
        Pageable pageable,
        boolean isAdmin,
        String userLogin,
        MessageType type
    ) {
        String loginToUse = isAdmin ? null : userLogin;

        return sendSmsRepository.findFiltered(loginToUse, search, isSent, isBulk, receiver, receivers, type, pageable);
    }

    @Override
    public Optional<SendSms> partialUpdate(SendSms sendSms) {
        log.debug("Request to partially update SendSms : {}", sendSms);

        return sendSmsRepository
            .findById(sendSms.getId())
            .map(existingSendSms -> {
                if (sendSms.getSender() != null) {
                    existingSendSms.setSender(sendSms.getSender());
                }
                if (sendSms.getReceiver() != null) {
                    existingSendSms.setReceiver(sendSms.getReceiver());
                }
                if (sendSms.getMsgdata() != null) {
                    existingSendSms.setMsgdata(sendSms.getMsgdata());
                }
                if (sendSms.getSendateEnvoi() != null) {
                    existingSendSms.setSendateEnvoi(sendSms.getSendateEnvoi());
                }
                if (sendSms.getDialogue() != null) {
                    existingSendSms.setDialogue(sendSms.getDialogue());
                }
                if (sendSms.getIsSent() != null) {
                    existingSendSms.setIsSent(sendSms.getIsSent());
                }
                if (sendSms.getIsbulk() != null) {
                    existingSendSms.setIsbulk(sendSms.getIsbulk());
                }
                if (sendSms.getTitre() != null) {
                    existingSendSms.setTitre(sendSms.getTitre());
                }

                return existingSendSms;
            })
            .map(sendSmsRepository::save);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SendSms> findAll(Pageable pageable) {
        log.debug("Request to get all SendSms");
        return sendSmsRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SendSms> findOne(Long id) {
        log.debug("Request to get SendSms : {}", id);
        return sendSmsRepository.findById(id);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete SendSms : {}", id);
        sendSmsRepository.deleteById(id);
    }
}
