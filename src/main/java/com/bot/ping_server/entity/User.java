package com.bot.ping_server.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.security.*;

@EntityScan
@Data
@Table(name = "user")
public class User extends JSONObject {
    public static String TYPE_STATUS_ONLINE = "ONLINE";
    public static String TYPE_STATUS_OFFLINE = "OFFLINE";

    @Setter
    @Getter
    public String uuid;

    @Setter
    @Getter
    @Column("name")
    public String name;

    @Setter
    @Getter
    @Column("email")
    public String email;

    @Setter
    @Getter
    @Column("nickname")
    public String nickname;

    @Setter
    @Getter
    @Column("description")
    public String description;

    @Setter
    @Getter
    private PrivateKey privateKey;

    @Setter
    @Getter
    private PublicKey publicKey;

    public void generateKeys(){
        try {
            KeyPairGenerator keyPairGenerator=KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);
            KeyPair pair = keyPairGenerator.generateKeyPair();

            privateKey=pair.getPrivate();
            publicKey=pair.getPublic();


        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


}
