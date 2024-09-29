package com.bot.ping_server.service;

import com.bot.ping_server.PingServerApplication;
import com.bot.ping_server.entity.User;
import com.bot.ping_server.repository.WsRepository;
import com.bot.ping_server.webSocket.WebSocketConnectInfo;
import com.bot.ping_server.webSocket.WebSocketType;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

@Service
public class UserService {

    public static User getUserByUuid(String uuid) throws SQLException, IOException {
        Connection connection = PingServerApplication.getConnection();

        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM "+PingServerApplication.config.getBdName()+".user WHERE uuid=?");
        preparedStatement.setString(1,uuid);
        ResultSet line = preparedStatement.executeQuery();

        String name = "";
        String email = "";
        String description = "";
        String nickname = "";
        User fullUser = new User();
        while (line.next()) {
            name=line.getString("name");
            email = line.getString("email");
            description = line.getString("description");
            nickname = line.getString("nickname");
        }

        line.close();
        preparedStatement.close();
        connection.close();

        fullUser.setUuid(uuid);
        fullUser.setName(name);
        fullUser.setEmail(email);
        fullUser.setDescription(description);
        fullUser.setNickname(nickname);



        return fullUser;
    }

    public static boolean checkEmail(String email) throws SQLException, IOException {
        Connection connection = PingServerApplication.getConnection();

        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM "+PingServerApplication.config.getBdName()+".user WHERE email=?");
        preparedStatement.setString(1,email);
        ResultSet line = preparedStatement.executeQuery();

        String password = "";
        while (line.next()) {
            password = line.getString("password_hash");
        }

        line.close();
        preparedStatement.close();
        connection.close();

        return !(password.isEmpty());
    }

    public static byte[] getAvatarByUuid(String uuid, String path) throws IOException {
        File file = new File(path+"//user-photos//"+uuid+".jpg");
        FileInputStream fileInputStream = new FileInputStream(file);
        return fileInputStream.readAllBytes();
    }

    public static String getUuidByEmail(String email) throws SQLException {
        Connection connection = PingServerApplication.getConnection();
        String uuid = "";
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM "+PingServerApplication.config.getBdName()+".user WHERE email=?");
        preparedStatement.setString(1,email);
        ResultSet line = preparedStatement.executeQuery();

        while (line.next()) {
            uuid = line.getString("uuid");
        }

        line.close();
        preparedStatement.close();
        connection.close();

        return uuid;
    }

    public static boolean checkContact(String uuid1, String uuid2) throws SQLException {
        Connection connection = PingServerApplication.getConnection();
        String uuid = "";

        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM "+PingServerApplication.config.getBdName()+".contacts WHERE user_uuid_1 in (?, ?) and  user_uuid_2 in (?, ?)");
        preparedStatement.setString(1,uuid1);
        preparedStatement.setString(2,uuid2);
        preparedStatement.setString(3,uuid1);
        preparedStatement.setString(4,uuid2);
        ResultSet line = preparedStatement.executeQuery();

        while (line.next()) {
            uuid = line.getString("user_uuid_1");
        }

        line.close();
        preparedStatement.close();
        connection.close();

        return !(uuid ==null);
    }

    public static ArrayList<User> getAllContacts(String uuid) throws SQLException, IOException {
        Connection connection = PingServerApplication.getConnection();

        ArrayList<User> allContacts = new ArrayList<>();

        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM "+PingServerApplication.config.getBdName()+".contacts WHERE user_uuid_1=? OR user_uuid_2=?");
        preparedStatement.setString(1,uuid);
        preparedStatement.setString(2,uuid);
        ResultSet line = preparedStatement.executeQuery();


        while (line.next()) {
            String uuid2 = line.getString("user_uuid_2");
            if (uuid.equals(uuid2))
                uuid2 = line.getString("user_uuid_1");
            User user = getUserByUuid(uuid2);
            allContacts.add(user);
        }

        line.close();
        preparedStatement.close();
        connection.close();

        return allContacts;
    }

    public static ArrayList<String> getAllContactsOnline(String uuid, WsRepository wsRepository) throws SQLException, IOException {
        ArrayList<User>contacts =getAllContacts(uuid);
        ArrayList<String> onlineUsers = new ArrayList<>();
        for (int i = 0; i <= contacts.size() - 1; i++) {
            User user = contacts.get(i);
            WebSocketConnectInfo webSocketConnectInfoContact = wsRepository.connects.get(user.getUuid());
            if (webSocketConnectInfoContact != null) {
                onlineUsers.add(user.getUuid());
            }
        }
        return onlineUsers;
    }

    public static void sendStatus(ArrayList<String> contacts, WsRepository wsRepository, String status, String uuid) throws JSONException, IOException {
        for (int i = 0; i <= contacts.size() - 1; i++) {
            WebSocketConnectInfo webSocketConnectInfoContact = wsRepository.connects.get(contacts.get(i));
            if (webSocketConnectInfoContact != null) {
                JSONObject jsonObjectMessage = new JSONObject();

                jsonObjectMessage.put("type", WebSocketType.TYPE_STATUS);
                jsonObjectMessage.put("uuid", uuid);
                jsonObjectMessage.put("status", status);


                webSocketConnectInfoContact.sendMessage(jsonObjectMessage.toString());
            }
        }
    }
}
