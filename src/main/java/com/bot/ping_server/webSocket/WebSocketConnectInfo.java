package com.bot.ping_server.webSocket;//package com.bot.ping_server.webSocket;

import com.bot.ping_server.entity.MyUser;
import com.bot.ping_server.entity.User;
import com.bot.ping_server.repository.WsRepository;
import com.bot.ping_server.service.UserService;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class WebSocketConnectInfo {

    public WebSocketConnectInfo(MyUser myUser, String idDevice, WebSocketSession webSocketSession){
        setMyUser(myUser);
        webSocketSessions.put(idDevice, webSocketSession);
    }

    @Getter
    @Setter
    public MyUser myUser;

    @Getter
    public HashMap<String, WebSocketSession> webSocketSessions=new HashMap<String, WebSocketSession>();

    public void sendMessage(String message, String id_device) throws IOException {
        HashMap<String, WebSocketSession> localWebSocketSessions=new HashMap<>();
        localWebSocketSessions.putAll(webSocketSessions);
        localWebSocketSessions.remove(id_device);
        WebSocketSession[] webSocketSessionArrayList=localWebSocketSessions.values().toArray(new WebSocketSession[0]);


        for(int i = 0;i <= webSocketSessionArrayList.length-1;i++){
            WebSocketSession webSocketSession=(WebSocketSession) webSocketSessionArrayList[i];
            webSocketSession.sendMessage(new TextMessage(message));
        }
    }

    public void sendMessage(String message) throws IOException {
        for(int i = 0;i <= webSocketSessions.size()-1;i++){
            WebSocketSession webSocketSession=(WebSocketSession) webSocketSessions.values().toArray()[i];
            webSocketSession.sendMessage(new TextMessage(message));
        }
    }


    public void disconnectWebsocket(WebSocketSession webSocketSession, WsRepository wsRepository) throws SQLException, IOException, JSONException {
        if(webSocketSessions.size()!=1) {
            for (int i = 0;i <= webSocketSessions.size()-1;i++){
                String id_device = (String) webSocketSessions.keySet().toArray()[i];
                if(webSocketSessions.get(id_device).equals(webSocketSession)){
                    webSocketSessions.remove(id_device);
                    break;
                }
            }
        }
        else {
            ArrayList<String> contacts = UserService.getAllContactsOnline(myUser.getUuid(), wsRepository);
            UserService.sendStatus(contacts, wsRepository, User.TYPE_STATUS_OFFLINE, myUser.getUuid());

            wsRepository.connects.remove(myUser.getUuid());
            for(int i = 0;i <= wsRepository.arrayQueue.size()-1;i++){
                if (wsRepository.arrayQueue.get(i).equals(myUser.getUuid())){
                    wsRepository.arrayQueue.remove(i);
                    break;
                }
            }
        }

    }

    public static String getUuidBySession(WebSocketSession webSocketSession,  WsRepository wsRepository){
        for(int i = 0;i <= wsRepository.arrayQueue.size()-1;i++) {
            String current_uuid =wsRepository.arrayQueue.get(i);
            HashMap<String, WebSocketSession> webSocketSessionUser=wsRepository.connects.get(current_uuid).webSocketSessions;
            for (int b = 0;b <= webSocketSessionUser.size()-1;b++){
                if(webSocketSession.equals(webSocketSessionUser.values().toArray()[b])){
                    return  wsRepository.arrayQueue.get(i);
                }
            }
        }
        return "";
    }

    public boolean checkSession(WebSocketSession webSocketSession) {
        for (int i = 0;i <= webSocketSessions.size()-1;i++){
            if(webSocketSession.equals(webSocketSessions.values().toArray()[i])){
                return true;
            }
        }
        return false;
    }
}
