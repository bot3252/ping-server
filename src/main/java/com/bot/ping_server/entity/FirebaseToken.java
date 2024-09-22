package com.bot.ping_server.entity;

import lombok.Getter;
import lombok.Setter;

public class FirebaseToken {
    @Getter
    @Setter
    String idDevise;

    @Getter
    @Setter
    String firebaseToken;

    @Getter
    @Setter
    String dateCreate;

    @Getter
    @Setter
    String uuidUser;

    public FirebaseToken(String uuidUser,String idDevise,String firebaseToken,String dateCreate){
        this.uuidUser=uuidUser;
        this.idDevise=idDevise;
        this.firebaseToken=firebaseToken;
        this.dateCreate=dateCreate;
    }
}