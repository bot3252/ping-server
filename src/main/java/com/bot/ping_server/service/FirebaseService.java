package com.bot.ping_server.service;

import com.bot.ping_server.PingServerApplication;
import com.bot.ping_server.entity.FirebaseToken;
import com.bot.ping_server.entity.NotificationMessage;
import com.bot.ping_server.utils.Log;
import com.bot.ping_server.utils.Logger;
import com.google.firebase.messaging.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

@Service
public class FirebaseService {
    @Autowired
    private FirebaseMessaging firebaseMessaging;

    String bd_name = PingServerApplication.getConfig().getBdName();

    public ArrayList<FirebaseToken> getTokens(String uuid) throws SQLException {
        Connection connection = PingServerApplication.getConnection();

        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM "+bd_name+".firebase_token WHERE uuid_user=?");
        preparedStatement.setString(1, uuid);
        ResultSet line = preparedStatement.executeQuery();

        ArrayList<FirebaseToken> tokens = new ArrayList<>();
        while (line.next()) {
            FirebaseToken firebaseToken=new FirebaseToken(line.getString("uuid_user"),line.getString("id_device"), line.getString("token"), line.getString("last_verification_date"));

            tokens.add(firebaseToken);
        }
        return tokens;
    }


    public void deleteTokenByToken(String token) throws SQLException {
        Connection connection = PingServerApplication.getConnection();

        PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM "+bd_name+".firebase_token WHERE token=?");
        preparedStatement.setString(1, token);
        int result = preparedStatement.executeUpdate();
    }

    public void deleteTokenByUuid(String uuid) throws SQLException {
        Connection connection = PingServerApplication.getConnection();

        PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM "+bd_name+".firebase_token WHERE uuid_user = ?");
        preparedStatement.setString(1, uuid);
        int result = preparedStatement.executeUpdate();

    }

    public void deleteTokenByIdDevice(String id) throws SQLException {
        Connection connection = PingServerApplication.getConnection();

        PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM "+bd_name+".firebase_token WHERE id_device = ?");
        preparedStatement.setString(1, id);
        int result = preparedStatement.executeUpdate();
    }

    public void setToken(String uuid, String token, String idDevice) throws SQLException {
        Connection connection = PingServerApplication.getConnection();
        SimpleDateFormat now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        now.setTimeZone(TimeZone.getTimeZone("UTC"));

        deleteTokenByIdDevice(idDevice);

        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO " + bd_name + ".firebase_token VALUES(?, ?, ?, ?)");
        preparedStatement.setString(1, token);
        preparedStatement.setString(2, uuid);
        preparedStatement.setString(3, now.format(new java.util.Date()));
        preparedStatement.setString(4, idDevice);

        int i = preparedStatement.executeUpdate();
    }

    public void sendNotificationByToken(NotificationMessage notificationMessage) {
        if(notificationMessage.getRecipientToken()!=null) {
            Notification notification = Notification
                    .builder()
                    .setTitle(notificationMessage.getTitle())
                    .setBody(notificationMessage.getBody())
                    .setImage(notificationMessage.getImage())
                    .build();

            for (int i=0;i<=notificationMessage.getRecipientToken().size()-1;i++) {
                Message message=Message.builder()
                        .setNotification(notification)
                        .putAllData(notificationMessage.getData())
                        .setToken(notificationMessage.getRecipientToken().get(i))
                        .build();
                try {
                    firebaseMessaging.send(message);
                } catch (FirebaseMessagingException e) {

                }
            }
        }
    }
}
