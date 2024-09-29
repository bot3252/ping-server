package com.bot.ping_server.controller;

import com.bot.ping_server.PingServerApplication;
import com.bot.ping_server.entity.FirebaseToken;
import com.bot.ping_server.entity.Message;
import com.bot.ping_server.entity.MyUser;
import com.bot.ping_server.entity.NotificationMessage;
import com.bot.ping_server.service.FirebaseService;
import com.bot.ping_server.service.LoginService;
import com.bot.ping_server.service.MediaService;
import com.bot.ping_server.utils.Log;
import com.bot.ping_server.utils.Logger;
import com.bot.ping_server.utils.Parser;
import com.bot.ping_server.webSocket.WebSocketConnectInfo;
import com.bot.ping_server.webSocket.WebSocketType;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.TextMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequestMapping("/message")
@Controller
public class MessageController {
    @Lazy
    @Autowired
    public final LoginService loginService;

    public String bd_name = PingServerApplication.config.getBdName();
    String path = PingServerApplication.getConfig().getPathData();

    @Lazy
    @Autowired
    public final FirebaseService firebaseMessagingService;

    public MessageController(@Lazy LoginService loginService, @Lazy FirebaseService firebaseMessagingService) throws SQLException {
        this.firebaseMessagingService = firebaseMessagingService;
        this.loginService = loginService;
    }
    @ResponseBody
    @GetMapping(value = "/getAllMessage")
    public ArrayList<Message> getMessages(@RequestParam(value = "to")  String to, @RequestParam(value = "from") String from, @RequestParam(value = "password") String password) throws SQLException, JSONException {
        Connection connection = PingServerApplication.getConnection();
        Log log = new Log("get messages");
        Logger.logEvent(log, "Starting....");

        if (loginService.checkLogin(from, password)){
            Statement statement = connection.createStatement();
            ResultSet line = statement.executeQuery("SELECT * FROM "+bd_name+".messages where to_ in ('"+to+"', '"+from+"') and from_ in ('"+to+"', '"+from+"')  order by date");
            int i = 0;

            ArrayList<Message> messages = new ArrayList<Message>();

            while (line.next()) {
                if (from.equals(line.getString("from_"))||from.equals(line.getString("to_"))){
                    i++;
                    Message message = new Message();

                    message.setMessage(line.getString("message"));
                    message.setDate(line.getString("date"));
                    message.setFrom(line.getString("from_"));
                    message.setTo(line.getString("to_"));
                    message.setUuid(line.getString("uuid"));

                    messages.add(message);
                }
            }

            line.close();
            statement.close();
            connection.close();

            Logger.logEvent(log, "DONE! messages:"+messages.size());
            return messages;
        }

        Logger.logError(log, "PASSWORD or EMAIL WRONG");
        return null;
    }

    @ResponseBody
    @PostMapping(value = "/sendMessage")
    public Message sendMessage(@RequestBody Message message,@RequestParam(value = "password")  String password,@RequestParam(value = "id_device")  String id_device) throws SQLException, JSONException, IOException {
        MyUser myUser = loginService.login(message.getFrom(), password);

        Connection connection = PingServerApplication.getConnection();


        Log log = new Log("sendMessage");
        Logger.logEvent(log, "Starting....");

        Instant instant1 = Instant.now();
        Instant instant2 = Instant.from(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.of("UTC"))
                        .parse(message.getDate()));

        if (((int)instant1.getEpochSecond() - (int) instant2.getEpochSecond()) / 60/5<2){

            if (myUser!=null) {
                message.setUuid(UUID.randomUUID().toString());

                Map<String, String> data = new HashMap<>();
                data.put("chat_user_uuid", message.getTo());

                ArrayList<FirebaseToken> tokens = firebaseMessagingService.getTokens(message.getTo());
                ArrayList<String> tokensString=new ArrayList<>();

                for(int i=0; i<= tokens.size()-1;i++){
                   tokensString.add(tokens.get(i).getFirebaseToken());
                }

                if(!tokens.isEmpty()) {
                    NotificationMessage notificationMessage = new NotificationMessage(tokensString, myUser.getName(), message.getMessage(), data, "");
                    firebaseMessagingService.sendNotificationByToken(notificationMessage);
                }

                WebSocketConnectInfo webSocketConnectInfoTo = PingServerApplication.getWsRepository().connects.get(message.getTo());
                WebSocketConnectInfo webSocketConnectInfoFrom = PingServerApplication.getWsRepository().connects.get(message.getFrom());

                PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO "+bd_name+".messages VALUES(?, ?, ?, ?, ?, ?)");
                preparedStatement.setString(1, message.getMessage());
                preparedStatement.setString(2, message.getTo());
                preparedStatement.setString(3, message.getFrom());
                preparedStatement.setString(4, message.getDate());
                preparedStatement.setString(5, message.getUuid());
                preparedStatement.setString(6, Message.MESSAGE_STATUS_SENT);

                int i = preparedStatement.executeUpdate();

                preparedStatement.close();
                connection.close();

                if(connection.isClosed());

                if (webSocketConnectInfoTo != null){
                    JSONObject jsonObject = Parser.parseMessageToJson(message);
                    jsonObject.put("type", WebSocketType.TYPE_MESSAGE);
                    webSocketConnectInfoTo.sendMessage(jsonObject.toString());
                }
                if(webSocketConnectInfoFrom!=null&&webSocketConnectInfoFrom.webSocketSessions.size()!=1){
                    JSONObject jsonObject = Parser.parseMessageToJson(message);
                    jsonObject.put("type", WebSocketType.TYPE_MESSAGE);
                    webSocketConnectInfoFrom.sendMessage(jsonObject.toString(), id_device);
                }
                Logger.logEvent(log, "DONE! MESSAGE:"+message.getMessage()+" USER:"+myUser);
                return message;
            }

            Logger.logError(log, "PASSWORD or EMAIL WRONG");
            return null;
        }

        Logger.logError(log, "User"  + message.getFrom() + " HAVE TIME ERROR");
        return null;
    }

    @ResponseBody
    @GetMapping(value = "/getMessageSortByDate")
    public ArrayList<Message> getMessageSortByDate(@RequestParam String uuid, @RequestParam String password, @RequestParam String chatUserUuid, @RequestParam String startData) throws SQLException, ParseException, JSONException {
        Connection connection = PingServerApplication.getConnection();
        Log log = new Log("get messages");
        Logger.logEvent(log, "Starting....");

        if (loginService.checkLogin(uuid, password)){

            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM "+bd_name+".messages WHERE to_ in (?, ?) and from_ in (?, ?) and date > '"+startData+"' ORDER BY date asc");
            preparedStatement.setString(1, uuid);
            preparedStatement.setString(2, chatUserUuid);
            preparedStatement.setString(3, uuid);
            preparedStatement.setString(4, chatUserUuid);

            ResultSet line = preparedStatement.executeQuery();

            int i = 0;

            ArrayList<Message> messages = new ArrayList<Message>();

            while (line.next()) {
                if (uuid.equals(line.getString("from_"))||uuid.equals(line.getString("to_"))){
                    i++;

                    Message message = new Message();

                    message.setMessage(line.getString("message"));
                    message.setDate(line.getString("date"));
                    message.setFrom(line.getString("from_"));
                    message.setTo(line.getString("to_"));
                    message.setUuid(line.getString("uuid"));

                    messages.add(message);
                }
            }

            line.close();
            preparedStatement.close();
            connection.close();

            Logger.logEvent(log, "DONE! messages:"+messages.size());
            return messages;
        }

        Logger.logError(log, "PASSWORD or EMAIL WRONG");
        return null;
    }

    @ResponseBody
    @PostMapping("/uploadMessageFile")
    public String save_file(@RequestParam("avatar") MultipartFile file, @RequestParam("uuid_user") String uuid_user,
                            @RequestParam("uuid_user") String uuid_file, @RequestParam("password") String password) throws IOException, SQLException {
        if (loginService.checkLogin(uuid_user, password)) {
            MediaService.saveFile(file, uuid_file);
        }
        return "done";
    }

    @ResponseBody
    @GetMapping("/getMessageFile")
    public ResponseEntity<ByteArrayResource> getMessageFile(@RequestParam("uuid_media") String uuid_media, @RequestParam("uuid_user") String uuid_user, @RequestParam("password") String password) throws Exception {
        File file = MediaService.getDownloadFile(uuid_media);

        HttpHeaders headers = new HttpHeaders();
        ByteArrayResource resource = null;

        if(loginService.checkLogin(uuid_user, password)) {
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");


            Path path = Paths.get(file.getAbsolutePath());
            resource = new ByteArrayResource(Files.readAllBytes(path));
        }

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}