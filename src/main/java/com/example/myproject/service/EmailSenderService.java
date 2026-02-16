package com.example.myproject.service;

import com.example.myproject.domain.ChannelConfiguration;
import java.util.Properties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {

    public void sendTestEmail(ChannelConfiguration cfg, String decryptedPassword) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("smtp.gmail.com");
        sender.setPort(587);
        sender.setUsername(cfg.getUsername());
        sender.setPassword(decryptedPassword);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.debug", true);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(cfg.getUsername());
        msg.setTo(cfg.getUsername()); // email réel
        msg.setSubject("EMAIL TEST OK");
        msg.setText("Configuration Email valide");

        sender.send(msg);
    }
}
