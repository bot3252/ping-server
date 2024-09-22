package com.bot.ping_server.utils;

import com.bot.ping_server.entity.Message;
import com.bot.ping_server.entity.MyUser;
import com.bot.ping_server.entity.User;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.MediaType;

import java.util.ArrayList;

public class Parser {
    public static MyUser parseJsonToMyUser(JSONObject jsonObject) throws JSONException {
        MyUser myUser = new MyUser();

        myUser.setUuid(String.valueOf(jsonObject.get("uuid")));
        myUser.setName(String.valueOf(jsonObject.get("name")));
        myUser.setEmail(String.valueOf(jsonObject.get("email")));
        myUser.setNickname(String.valueOf(jsonObject.get("nickname")));

        myUser.setDescription(String.valueOf(jsonObject.get("description")));

        if(jsonObject.get("password")!=null)
            myUser.setPassword(String.valueOf(jsonObject.get("password")));
        if(jsonObject.get("password_hash")!=null)
            myUser.setPassword_hash(String.valueOf(jsonObject.get("password_hash")));

        return myUser;
    }

    public static User parseJsonToUser(JSONObject jsonObject) throws JSONException {
        User user = new User();

        user.setUuid(String.valueOf(jsonObject.get("uuid")));
        user.setName(String.valueOf(jsonObject.get("name")));
        user.setEmail(String.valueOf(jsonObject.get("email")));
        user.setNickname(String.valueOf(jsonObject.get("nickname")));
        user.setDescription(String.valueOf(jsonObject.get("description")));

        return user;
    }

    public static JSONObject parseMessageToJson(Message message) throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("message", message.getMessage());
        jsonObject.put("to", message.getTo());
        jsonObject.put("from", message.getFrom());
        jsonObject.put("date", message.getDate());
        jsonObject.put("uuid", message.getUuid());

        return jsonObject;
    }

    public static JSONObject parseUserToJson(User user) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uuid", user.getUuid());
        jsonObject.put("name", user.getName());
        jsonObject.put("email", user.getEmail());
        jsonObject.put("nickname", user.getNickname());
        jsonObject.put("description", user.getDescription());
        return jsonObject;
    }

    public static String getMediaType(String extension){
        switch (extension){
            case "jpg":
                return MediaType.IMAGE_JPEG_VALUE;
            case "png":
                return MediaType.IMAGE_PNG_VALUE;
            case "gif":
                return MediaType.IMAGE_GIF_VALUE;
            case "mp3":
                return "video/mp4";
        }
        return "not_found";
    }

}
