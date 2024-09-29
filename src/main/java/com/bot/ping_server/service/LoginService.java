package com.bot.ping_server.service;

import com.bot.ping_server.PingServerApplication;
import com.bot.ping_server.entity.MyUser;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Objects;

import static java.lang.String.format;
@Service
public class LoginService {
    String bd_name = PingServerApplication.getConfig().getBdName();
    public LoginService() throws SQLException {
    }

    public MyUser login(String uuid, String password) throws SQLException {
        Connection connection = PingServerApplication.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM "+bd_name+".user WHERE uuid=?");
        preparedStatement.setString(1,uuid);
        ResultSet line = preparedStatement.executeQuery();

        String name = "";
        String email = "";
        String description = "";
        String nickname = "";
        String passwordHash = "";
        String status = "";
        while (line.next()) {
            name=line.getString("name");
            email = line.getString("email");
            description = line.getString("description");
            nickname = line.getString("nickname");
            passwordHash = line.getString("password_hash");
        }

        line.close();
        preparedStatement.close();
        connection.close();

        if(Objects.equals(passwordHash, hashingPassword(password))) {
            MyUser fullUser = new MyUser();
            fullUser.setUuid(uuid);
            fullUser.setName(name);
            fullUser.setEmail(email);
            fullUser.setDescription(description);
            fullUser.setNickname(nickname);
            fullUser.setPassword_hash(passwordHash);
            return fullUser;
        }
        return null;
    }
    public boolean  checkLogin(String uuid, String password) throws SQLException {
        return login(uuid, password)!=null;
    }

    public static String hashingPassword(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] hash = md.digest(password.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(format("%02x", b));
        }
        return hexString.toString();
    }
}
