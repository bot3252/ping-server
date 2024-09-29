package com.bot.ping_server;

import com.bot.ping_server.entity.Config;
//import com.bot.ping_server.repository.WsRepository;
import com.bot.ping_server.entity.FirebaseToken;
import com.bot.ping_server.entity.NotificationMessage;
import com.bot.ping_server.repository.WsRepository;
import com.bot.ping_server.service.FirebaseService;
import com.bot.ping_server.utils.Log;
import com.bot.ping_server.utils.Logger;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.Getter;
import lombok.SneakyThrows;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Lazy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
public class PingServerApplication implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

	@Getter
	public static Config config;

    @Getter
    @Lazy
    public static WsRepository wsRepository = new WsRepository();

    static Connection connection;

    @Bean
	FirebaseMessaging firebaseMessaging() throws IOException {
		FileInputStream serviceAccount = new FileInputStream(config.getPathData()+"//ping-7731d-firebase-adminsdk-cffpt-c0c0bb89e3.json");
		FirebaseOptions options = new FirebaseOptions.Builder()
				.setCredentials(GoogleCredentials.fromStream(serviceAccount))
				.build();
		FirebaseApp app = FirebaseApp.initializeApp(options);

		return FirebaseMessaging.getInstance(app);
	}

	public static void main(String[] args) throws FileNotFoundException, JSONException, SQLException {
		config = updateConfig(args[0]);
		SpringApplication.run(PingServerApplication.class, args);
	//	connection=DriverManager.getConnection("jdbc:mysql://"+config.getBdIp()+":"+config.getBdPort()+"/"+config.getBdName()+"?autoReconnect=true", config.getUserName(), config.getBdPassword());
    }

	static Config updateConfig(String path) throws FileNotFoundException, JSONException {
		File file = new File(path);
		Scanner scanner = new Scanner(file);
		String string = "";
		while (scanner.hasNext()){
			string+=scanner.next();
		}
		JSONObject jsonObject = new JSONObject(string);
		return new Config(jsonObject.getString("path_data"),jsonObject.getString("ip_server"), jsonObject.getString("port_server"), jsonObject.getString("bd_ip"),jsonObject.getString("bd_port"),  jsonObject.getString("bd_name"), jsonObject.getString("bd_user_name"), jsonObject.getString("bd_password"));
	}


	@SneakyThrows
	@Override
	public void customize(TomcatServletWebServerFactory factory) {
		factory.setPort(Integer.parseInt(config.getServerPort()));
		if (!config.getServerIp().isEmpty()) {
			factory.setAddress(InetAddress.getByName(config.getServerIp()));
		}
	}

	public static Connection getConnection() {
        try {
            connection=DriverManager.getConnection("jdbc:mysql://"+config.getBdIp()+":"+config.getBdPort()+"/"+config.getBdName()/*+"?autoReconnect=true"*/, config.getUserName(), config.getBdPassword());
        	return connection;
		} catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }
}