package com.bot.ping_server.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationMessage {

    public NotificationMessage(){

    }

    public NotificationMessage(List<String> recipientToken, String title, String body, Map<String, String> data,String image){
        ArrayList<String> normalTokens = new ArrayList<>();
        for(int i = 0; i<=recipientToken.size()-1; i++){
            if(recipientToken.get(i)!=null)
                normalTokens.add(recipientToken.get(i));
        }
        this.setRecipientToken(normalTokens);
        this.setTitle(title);
        this.setBody(body);
        this.setImage(image);
        this.setData(data);
    }
    @Column("recipientToken")
    @Getter
    @Setter
    private List<String> recipientToken;

    @Column("title")
    @Getter
    @Setter
    private String title;

    @Column("body")
    @Getter
    @Setter
    private String body;

    @Column("image")
    @Getter
    @Setter
    private String image;

    @Column("data")
    @Getter
    @Setter
    private Map<String, String> data;
}
