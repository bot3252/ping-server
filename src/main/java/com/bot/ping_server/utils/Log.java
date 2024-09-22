package com.bot.ping_server.utils;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


public class Log {
    public Log(String event) {
        id = UUID.randomUUID().toString();
        this.event=event;
        this.message=message;
    }
    @Getter
    @Setter
    public String id;
    @Getter
    @Setter
    public String event;
    @Getter
    @Setter
    public String message;
}
