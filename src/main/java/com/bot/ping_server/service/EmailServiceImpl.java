package com.bot.ping_server.service;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Properties;

@Service
public class EmailServiceImpl implements EmailService {

//    @Autowired
//    public JavaMailSender emailSender;


    @Override
    public void sendSimpleMessage(String to, String title, String message) throws MessagingException {
        Properties props;
        props = new Properties();

        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.from", "ping.chat.noreply@gmail.com");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.port", "587");
        props.setProperty("mail.debug", "true");

        Session session = Session.getInstance(props, null);
        MimeMessage msg = new MimeMessage(session);

        msg.setRecipients(Message.RecipientType.TO, to);
        msg.setSubject(title);
        msg.setSentDate(new Date());
        msg.setText(message);

        Transport transport = session.getTransport("smtp");

        transport.connect("ping.chat.noreply@gmail.com", "ylro lpre xuls zyga");
        transport.sendMessage(msg, msg.getAllRecipients());
        transport.close();

    }
}