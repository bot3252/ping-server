package com.bot.ping_server.controller;

import com.bot.ping_server.PingServerApplication;
import com.bot.ping_server.entity.MyUser;
import com.bot.ping_server.service.EmailServiceImpl;
import com.bot.ping_server.service.FirebaseService;
import com.bot.ping_server.service.LoginService;
import com.bot.ping_server.service.UserService;
import com.bot.ping_server.utils.Log;
import com.bot.ping_server.utils.Logger;
import com.bot.ping_server.webSocket.WebSocketConnectInfo;
import jakarta.mail.MessagingException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.bot.ping_server.PingServerApplication.wsRepository;

@Controller
@RequestMapping("/register")
public class RegisterUserController {
    ConcurrentHashMap<String,MyUser> verificationUsers;
    ConcurrentHashMap<String,String> forgetPasswordUsers;
    @Lazy
    @Autowired
    public final EmailServiceImpl emailSenderService;

    @Lazy
    @Autowired
    public final LoginService loginService;

    @Lazy
    @Autowired
    public final FirebaseService firebaseService;
    public String bd_name = PingServerApplication.config.getBdName();
    public RegisterUserController(@Lazy EmailServiceImpl emailSenderService, @Lazy LoginService loginService, @Lazy FirebaseService firebaseService) throws SQLException {
        this.emailSenderService = emailSenderService;
        this.loginService = loginService;
        this.firebaseService = firebaseService;

        verificationUsers=new ConcurrentHashMap<String,MyUser>();
        forgetPasswordUsers=new ConcurrentHashMap<String, String>();
    }
    @ResponseBody
    @PostMapping(value = "/register")
    public String registerUser(@RequestBody MyUser user) throws MessagingException, SQLException, IOException {
        Log log = new Log("registerUser");
        if(!UserService.checkEmail(user.getEmail())) {
            Logger.logEvent(log, "Starting....");

            String verificationCode = String.valueOf(generateCodeEnter());
            emailSenderService.sendSimpleMessage(user.email,
                    "Пожалуйста, подтвердите свою почту",
                    "Ваш разовый код:\n" + verificationCode + "\nЕсли вы не запрашивали этот код, можете смело игнорировать это сообщение электронной почты. Возможно, кто-то ввел ваш адрес электронной почты по ошибке😅.");
            user.setUuid(UUID.randomUUID().toString());
            user.setPassword_hash(LoginService.hashingPassword(user.password));
            verificationUsers.put(verificationCode, user);
            startDeleteVerificateCode(verificationCode);

            Logger.logEvent(log, "DONE! VerificationCode: " + verificationCode);
            return "successful";
        }
        Logger.logError(log, "this email is already in use");
        return "this email is already in use";
    }

    @ResponseBody
    @PostMapping(value = "/forgetPassword")
    public String forgetPassword(@RequestBody String email) throws MessagingException, SQLException, IOException, JSONException {
        Log log = new Log("forgetPassword");
        JSONObject jsonObject = new JSONObject(email);
        String emailString=jsonObject.getString("email");
        if(UserService.checkEmail(emailString)) {
            Logger.logEvent(log, "Starting....");

            String verificationCode = String.valueOf(generateCodeEnter());
            forgetPasswordUsers.put(verificationCode, emailString);
            emailSenderService.sendSimpleMessage(emailString,
                    "Кто-то запросил зброс пароля",
                    "Ваш разовый код:\n" + verificationCode + "\nЕсли вы не запрашивали этот код, можете смело игнорировать это сообщение электронной почты. Возможно, кто-то ввел ваш адрес электронной почты по ошибке😅.");

            Logger.logEvent(log, "DONE! verificationCode: " + verificationCode);

            startDeleteForgetCode(verificationCode);
            return "successful";
        }

        return "this email don`t use";
    }

    @ResponseBody
    @PostMapping(value = "/checkVereficateCodeForgetPassword")
    public String checkVereficateCodeForgetPassword(@RequestParam(value = "email") String email, @RequestParam(value = "newPassword") String newPassword,@RequestParam(value = "verificationCode") String verificationCode) throws MessagingException, SQLException, IOException {
        String emailString = forgetPasswordUsers.get(verificationCode);
        Log log = new Log("checkVereficateCodeForgetPassword");
        Logger.logEvent(log, "Starting....");
        if(emailString!=null) {
            if (emailString.equals(email)) {
                Connection connection = PingServerApplication.getConnection();
                Statement statement = connection.createStatement();
                int line = statement.executeUpdate("update "+bd_name+".user set password_hash = '"+LoginService.hashingPassword(newPassword) +"' where email = '"+email+"'");
                forgetPasswordUsers.remove(verificationCode);
                return "successful";
            }
            Logger.logError(log, "PASSWORD or EMAIL WRONG");
        }
        Logger.logError(log, "User DON`T CREATE in HASHMAP");
        return "error";
    }

    @ResponseBody
    @GetMapping(value = "/verificateAccount")
    public String verificateAccount(@RequestParam(value = "email") String email, @RequestParam(value = "verificationCode") String verificationCode) throws SQLException {
        MyUser user = verificationUsers.get(verificationCode);

        Connection connection = PingServerApplication.getConnection();

        Log log = new Log("verificateAccount");
        Logger.logEvent(log, "Starting....");

        if(user!=null) {
            if (user.getEmail().equals(email)) {
                PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO "+bd_name+".user VALUES(?, ?, ?, ?, ?, ?)");

                preparedStatement.setString(1, user.getUuid());
                preparedStatement.setString(2, user.getName());
                preparedStatement.setString(3, "@"+user.getName());
                preparedStatement.setString(4, user.getEmail());
                preparedStatement.setString(5, "Привет! Я использую ping.");
                preparedStatement.setString(6, LoginService.hashingPassword(user.getPassword()));
                int i = preparedStatement.executeUpdate();

                verificationUsers.remove(verificationCode);
                Logger.logEvent(log, "DONE! user: " + user);
                return "successful";
            }
            Logger.logError(log, "PASSWORD or EMAIL WRONG");
        }
        Logger.logError(log, "User DON`T CREATE in HASHMAP");
        return "null";
    }

    @ResponseBody
    @GetMapping(value = "/login")
    public MyUser loginUser(@RequestParam(value = "email") String email, @RequestParam(value = "password") String password, @RequestParam(value = "token") String token, @RequestParam(value = "id_devise") String id_devise) throws MessagingException, SQLException, IOException {
        MyUser user = loginService.login(UserService.getUuidByEmail(email), password);

        Log log = new Log("loginUser");
        Logger.logEvent(log, "Starting....");

        if(user.getPassword_hash().equals(LoginService.hashingPassword(password))){
            user.setPassword(password);
            firebaseService.setToken(UserService.getUuidByEmail(email),token,id_devise);
            Logger.logEvent(log, "DONE! user: " + user);
            return user;
        }
        Logger.logError(log, "PASSWORD or EMAIL WRONG");
        return null;
    }

    @ResponseBody
    @PostMapping(value = "/logout")
    public String logout(@RequestParam(value = "firebaseToken") String firebaseToken, @RequestBody MyUser myUser) throws MessagingException, SQLException {

        Log log = new Log("logout");
        Logger.logEvent(log, "Starting....");

        if (loginService.checkLogin(myUser.getUuid(), myUser.getPassword())) {
            firebaseService.deleteTokenByToken(firebaseToken);
            Logger.logEvent(log, "DONE! User" + myUser.getUuid());
            return "successful";
        }
        Logger.logError(log, "PASSWORD or EMAIL WRONG");
        return "error";
    }

    @ResponseBody
    @PostMapping(value = "/deleteAccount")
    public String deleteAccount(@RequestBody MyUser myUser) throws MessagingException, SQLException {
        MyUser user = loginService.login(myUser.getEmail(), myUser.getPassword());

        Log log = new Log("deleteAccount");
        Logger.logEvent(log, "Starting....");

        if (user!=null) {
            firebaseService.deleteTokenByUuid(myUser.getUuid());

            Logger.logEvent(log, "DONE! User" + user+ " DELETED!");
            return "successful";
        }
        Logger.logError(log, "PASSWORD or EMAIL WRONG");
        return "error";
    }

    private void startDeleteVerificateCode(String code) {
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        verificationUsers.remove(code);
                    }
                },
                1000 * 60 * 5
        );
    }

    private void startDeleteForgetCode(String code) {
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        forgetPasswordUsers.remove(code);
                    }
                },
                1000 * 60 * 5
        );
    }


    private int generateCodeEnter(){
        int codeEnter = 0;
        Random random = new Random();
        for(int countNumber = 1; countNumber<=4; countNumber++){
            int i = 0;
            i = random.nextInt(9);
            for (int a = 1; a<=countNumber;a++){
                i=i*10;
            }
            codeEnter+=i;
        }
        return codeEnter;
    }
}