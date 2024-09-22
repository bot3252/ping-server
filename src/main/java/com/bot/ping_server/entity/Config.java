package com.bot.ping_server.entity;

import lombok.Getter;
import lombok.Setter;

public class Config {

    @Setter
    @Getter
    public String pathData;

    @Getter
    @Setter
    public String serverIp, serverPort;

    @Getter
    @Setter
    public String bdPort, bdName, bdIp,userName, bdPassword;

    public Config(String pathData,String serverIp, String serverPort, String bdIp,String bdPort, String bdName,String bdUserName, String bdPassword){
        setPathData(pathData);

        setServerIp(serverIp);
        setServerPort(serverPort);

        setBdIp(bdIp);
        setBdPort(bdPort);
        setBdName(bdName);
        setUserName(bdUserName);
        setBdPassword(bdPassword);
    }
}
