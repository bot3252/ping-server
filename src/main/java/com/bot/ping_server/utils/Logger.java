package com.bot.ping_server.utils;

public class Logger {
    public static void logEvent(Log log, String message){
        System.out.println("Event "+"id "+log.id+" '"+log.event+"' message: "+ message);
    }

    public static void logError(Log log, String message){
        System.out.println("ERROR "+"id "+log.id+" '"+log.event+"' message:" + message);
    }
}
