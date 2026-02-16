package com.example.myproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class EncryptionConfiguration {

    @Bean
    public TextEncryptor textEncryptor() {
        String salt = "1234567890abcdef";
        String password = "mySuperSecretPassword";
        return Encryptors.text(password, salt);
    }
}
