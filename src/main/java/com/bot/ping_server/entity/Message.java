package com.bot.ping_server.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

public class Message {
    public static String MESSAGE_STATUS_SENT="SENT";
    public static String MESSAGE_STATUS_DELIVERED="DELIVERED";
    public static String MESSAGE_STATUS_READED="READED";

    @Setter
    @Getter
    @Column("uuid")
    public String uuid;

    @Setter
    @Getter
    @Column("message")
    public String message;

    @Setter
    @Getter
    @Column("from")
    public String from;

    @Setter
    @Getter
    @Column("to")
    public String to;

    @Setter
    @Getter
    @Column("date")
    public String date;

//    @Setter
//    @Getter
//    public ArrayList<String> uuids_media;
//
//    @Getter
//    @Setter
//    @Column("media")
//    public ArrayList<MultipartFile> mediaArrayList;
}
