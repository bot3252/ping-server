package com.bot.ping_server.repository;

import com.bot.ping_server.webSocket.WebSocketConnectInfo;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class WsRepository {
    public ConcurrentHashMap<String, WebSocketConnectInfo>  connects;
    public CopyOnWriteArrayList<String> arrayQueue;
    public WsRepository(){
        connects = new ConcurrentHashMap<>();
        arrayQueue = new CopyOnWriteArrayList<>();
    }
}
