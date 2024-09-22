package com.bot.ping_server.service;

import jakarta.mail.MessagingException;

public interface EmailService {
    public void sendSimpleMessage(String to, String title, String message) throws MessagingException;
}