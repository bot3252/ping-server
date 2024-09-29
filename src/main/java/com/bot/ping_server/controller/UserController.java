package com.bot.ping_server.controller;

import com.bot.ping_server.PingServerApplication;
import com.bot.ping_server.entity.Contact;
import com.bot.ping_server.entity.MyUser;
import com.bot.ping_server.entity.User;
import com.bot.ping_server.service.LoginService;
import com.bot.ping_server.service.UserService;
import com.bot.ping_server.utils.ImageUtil;
import com.bot.ping_server.utils.Log;
import com.bot.ping_server.utils.Logger;
import com.bot.ping_server.utils.Parser;
import com.bot.ping_server.webSocket.WebSocketConnectInfo;
import com.bot.ping_server.webSocket.WebSocketType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.TextMessage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Objects;

import static com.bot.ping_server.PingServerApplication.wsRepository;

@RequestMapping("/user")
@Controller
public class UserController {
    @Lazy
    @Autowired(required = true)
    public final LoginService loginService;
    @Lazy
    @Autowired(required = true)
    public final UserService userService;

    String bd_name = PingServerApplication.getConfig().getBdName();
    String path = PingServerApplication.getConfig().getPathData();

    public UserController(LoginService loginService, UserService userService) throws SQLException {
        this.loginService = loginService;
        this.userService = userService;
    }

    @GetMapping(
            value = "/getAvatar",
            produces = MediaType.IMAGE_JPEG_VALUE
    )
    public @ResponseBody byte[] getAvatar(@RequestParam(value = "uuid") String uuid) throws IOException {
        Log log = new Log("getAvatar");
        Logger.logEvent(log, "Starting....");

        byte[] avatar = UserService.getAvatarByUuid(uuid, path);

        Logger.logEvent(log, "DONE! User " + uuid + " GET AVATAR");
        return avatar;
    }

    @ResponseBody
    @PostMapping("/uploadAvatar")
    public String uploadAvatar(@RequestParam("uuid") String uuid,
                               @RequestParam("password") String password,
                               @RequestParam("avatar") MultipartFile file) throws IOException, SQLException {
        Log log = new Log("uploadAvatar");
        Logger.logEvent(log, "Starting....");

        MyUser myUser = loginService.login(uuid, password);
        if (myUser != null) {
            StringBuilder fileNames = new StringBuilder();
            Path fileNameAndPath = Paths.get(path       + "//user-photos", uuid + ".jpg");
            fileNames.append(file.getOriginalFilename());
            BufferedImage bufferImage = ImageUtil.resizeImage(ImageIO.read(file.getInputStream()), 248, 248);


            Files.write(fileNameAndPath, ImageUtil.toByteArray(bufferImage, "jpg"));

            Logger.logEvent(log, "DONE! User " + myUser);
            return "successful";
        }
        Logger.logError(log, "PASSWORD or EMAIL WRONG");
        return "wrong password or email";
    }

    @ResponseBody
    @PostMapping("/createContact")
    public String createContact(@RequestBody Contact contact,
                                @RequestParam("password") String password) throws IOException, SQLException, JSONException {
        Log log = new Log("createContact");
        Logger.logEvent(log, "Starting....");
        Connection connection = PingServerApplication.getConnection();
        if (loginService.checkLogin(contact.getUuid1(), password)) {
            if (UserService.checkContact(contact.uuid1, contact.uuid2)) {
                PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO " + bd_name + ".contacts (user_uuid_1, user_uuid_2) VALUES(?, ?)");

                preparedStatement.setString(1, contact.getUuid1());
                preparedStatement.setString(2, contact.getUuid2());
                int i = preparedStatement.executeUpdate();
                connection.close();

                WebSocketConnectInfo webSocketConnectInfo = PingServerApplication.getWsRepository().connects.get(contact.getUuid2());

                if (webSocketConnectInfo != null){
                    User user = UserService.getUserByUuid(contact.getUuid1());
                    JSONObject jsonObject = new JSONObject(Parser.parseUserToJson(user).toString());

                    jsonObject.put("type", WebSocketType.TYPE_NEW_CONTACT);

                    webSocketConnectInfo.sendMessage(jsonObject.toString());
                }

                Logger.logEvent(log, "DONE! User " + contact.getUuid1());
            }
            return "successful";
        }
        Logger.logError(log, "PASSWORD or EMAIL WRONG");
        return "wrong password or email";
    }

    @ResponseBody
    @GetMapping("/getAllContacts")
    public ArrayList<User> getAllContacts(@RequestParam("uuid") String uuid,
                                          @RequestParam("password") String password) throws IOException, SQLException {
        Log log = new Log("getAllContacts");
        Logger.logEvent(log, "Starting....");

        ArrayList<User> users = new ArrayList<User>();

        if (loginService.checkLogin(uuid, password)) {
            Connection connection = PingServerApplication.getConnection();

            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM "+bd_name+".contacts WHERE user_uuid_1= ? OR user_uuid_2= ?");
            preparedStatement.setString(1,uuid);
            preparedStatement.setString(2, uuid);
            ResultSet line = preparedStatement.executeQuery();

            while (line.next()) {
                String uuid2 = line.getString("user_uuid_2");
                if (Objects.equals(uuid, uuid2))
                    uuid2 = line.getString("user_uuid_1");
                User user = userService.getUserByUuid(uuid2);
                users.add(user);
            }
            Logger.logEvent(log, "DONE! User " + uuid + " RETURNED:" + users.size());
            return users;
        }
        Logger.logError(log, "PASSWORD or EMAIL WRONG");
        return null;
    }

    @ResponseBody
    @PostMapping(value = "/updateUser")
    public String setUser(@RequestBody MyUser user) throws SQLException {
        Log log = new Log("updateUser");
        Logger.logEvent(log, "Starting....");

        MyUser myUser = loginService.login(user.getUuid(), user.getPassword());
        if (loginService.checkLogin(user.getUuid(), user.getPassword())) {
            Connection connection = PingServerApplication.getConnection();

            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE "+ bd_name+".user SET name = ?, nickname= ? ,description= ? WHERE uuid = ?");
            preparedStatement.setString(1, user.getName());
            preparedStatement.setString(2, user.getNickname());
            preparedStatement.setString(3, user.getDescription());
            preparedStatement.setString(4, user.getUuid());

            preparedStatement.executeUpdate();


            preparedStatement.close();
            connection.close();

            Logger.logEvent(log, "DONE! User " + myUser);
        }
        Logger.logError(log, "user " + user);
        return "successful";
    }



    @ResponseBody
    @GetMapping("/findUser")
    public String findUser(@RequestParam("tag") String tag) throws IOException, SQLException, JSONException {
        Log log = new Log("findUser");
        Logger.logEvent(log, "Starting....");

        JSONArray users = new JSONArray();
        ArrayList<String> uuids = new ArrayList<String>();

        Connection connection = PingServerApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet line = statement.executeQuery("SELECT * FROM " + bd_name + ".user where name LIKE '%%" + tag + "%%' OR nickname LIKE '%%" + tag + "%%'");

        while (line.next()) {
            String uuid = line.getString("uuid");
            uuids.add(uuid);
        }

        line.close();
        statement.close();
        connection.close();


        for (int i = 0; i <= uuids.size() - 1; i++) {
            User user = UserService.getUserByUuid(uuids.get(i));
            JSONObject jsonObject = Parser.parseUserToJson(user);
            if(wsRepository.connects.get(user.getUuid())!=null){
                jsonObject.put(WebSocketType.TYPE_STATUS, User.TYPE_STATUS_ONLINE);
            }else {
                jsonObject.put(WebSocketType.TYPE_STATUS, User.TYPE_STATUS_OFFLINE);
            }
            users.put(jsonObject);
        }

        Logger.logEvent(log, "DONE! Users size " + users.length());
        return users.toString();
    }


    @ResponseBody
    @GetMapping("/getOnlineContacts")
    public ArrayList<String> getOnlineContacts(@RequestParam("uuid") String uuid, @RequestParam("password") String password) throws IOException, SQLException {
        Log log = new Log("getOnlineContacts");
        Logger.logEvent(log, "Starting....");
        ArrayList<String> onlineUsers = new ArrayList<>();
        if(loginService.checkLogin(uuid, password)) {

            onlineUsers = UserService.getAllContactsOnline(uuid, PingServerApplication.getWsRepository());

            Logger.logEvent(log, "DONE! uuid: "+uuid+" RETURNED:"+onlineUsers.size());
            return onlineUsers;
        }
        Logger.logEvent(log, "DONE! uuid: "+uuid+" RETURNED:"+onlineUsers.size());
        return null;
    }

    //    @ResponseBody
    //    @GetMapping("/getContactsCount")
    //    public Integer getCountContacts(@RequestParam("uuid") String uuid, @RequestParam("password") String password) throws SQLException, IOException {
    //        if(loginService.checkLogin(uuid, password)) {
    //            return UserService.getAllContacts(uuid).size();
    //        }
    //        return -1;
    //    }
}