package com.example.myproject.service;

import com.example.myproject.domain.ChannelConfiguration;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.smpp.SmppConstants;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

@Service
public class SmsSenderService {

    private final CamelContext camel;
    private final TextEncryptor encryptor;

    public SmsSenderService(CamelContext camel, TextEncryptor encryptor) {
        this.camel = camel;
        this.encryptor = encryptor;
    }

    public void test(ChannelConfiguration cfg) {
        String password = encryptor.decrypt(cfg.getEncryptedPassword());
        String host;

        switch (cfg.getSmsOperator()) {
            case "Mattel":
                host = "smpp.mattel.example.com";
                break;
            case "Mauritel":
                host = "smpp.mauritel.example.com";
                break;
            case "Chinguitel":
                host = "smpp.chinguitel.example.com";
                break;
            default:
                throw new IllegalArgumentException("Opérateur SMS inconnu");
        }

        String uri = "smpp://" + host + ":" + cfg.getPort() + "?systemId=" + cfg.getUsername() + "&password=" + password;
    }
}
