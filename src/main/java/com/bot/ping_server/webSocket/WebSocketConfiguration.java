package com.bot.ping_server.webSocket;
import com.bot.ping_server.PingServerApplication;
import com.bot.ping_server.entity.MyUser;
import com.bot.ping_server.entity.User;
import com.bot.ping_server.repository.WsRepository;
import com.bot.ping_server.service.LoginService;
import com.bot.ping_server.service.UserService;
import com.bot.ping_server.utils.Log;
import com.bot.ping_server.utils.Logger;
import org.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    LoginService loginService;

    {
        try {
            loginService = new LoginService();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    WsRepository wsRepository = PingServerApplication.getWsRepository();

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                Log log = new Log("webSocket");
                Logger.logEvent(log, "first connect");
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                Log log = new Log("webSocket connection....");
                Logger.logEvent(log, "Starting....");

                String messageString = (String) message.getPayload();
                JSONObject jsonObject = new JSONObject(messageString);

                MyUser myUser = loginService.login(jsonObject.getString("uuid"), jsonObject.getString("password"));
                if (myUser != null) {
                    WebSocketConnectInfo webSocketConnectInfo=wsRepository.connects.get(myUser.getUuid());
                    String id_device = jsonObject.getString("id_device");
                    if (webSocketConnectInfo!=null) {
                        wsRepository.connects.get(myUser.getUuid()).webSocketSessions.put(id_device,session);
                    }else{
                        webSocketConnectInfo = new WebSocketConnectInfo(myUser, id_device,session);
                        wsRepository.connects.put(myUser.getUuid(), webSocketConnectInfo);
                        wsRepository.arrayQueue.add(myUser.getUuid());
                    }
                    ArrayList<String> contacts = UserService.getAllContactsOnline(myUser.getUuid(), wsRepository);
                    UserService.sendStatus(contacts, wsRepository, User.TYPE_STATUS_ONLINE, jsonObject.getString("uuid"));

                    Logger.logEvent(log, "DONE! connected USER: " + myUser.getUuid());
                }else {
                    Logger.logError(log, "ERROR LOGIN");
                }
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                Log log = new Log("webSocket handleTransportError");
                Logger.logEvent(log, "starting....");

                String uuid = WebSocketConnectInfo.getUuidBySession(session, wsRepository);

                if(!uuid.isEmpty()) {
                    WebSocketConnectInfo webSocketConnectInfo = wsRepository.connects.get(uuid);
                    if (webSocketConnectInfo != null) {
                        if (webSocketConnectInfo.checkSession(session)) {
                            webSocketConnectInfo.disconnectWebsocket(session, wsRepository);
                        }
                    }
                }
                Logger.logEvent(log, "USER: "+uuid+"have error: "+exception.getMessage());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                Log log = new Log("webSocket after connection closed");
                Logger.logEvent(log, "starting....");

                String uuid = WebSocketConnectInfo.getUuidBySession(session, wsRepository);

                if(!uuid.isEmpty()) {
                    WebSocketConnectInfo webSocketConnectInfo = wsRepository.connects.get(uuid);
                    if (webSocketConnectInfo != null) {
                        if (webSocketConnectInfo.checkSession(session)) {
                            webSocketConnectInfo.disconnectWebsocket(session, wsRepository);
                        }
                    }
                }

                Logger.logEvent(log, "USER: "+uuid+" CLOSED");
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, "/websocket");
    }
}