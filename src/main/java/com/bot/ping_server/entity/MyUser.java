package com.bot.ping_server.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@EntityScan
@Data
@Table(name = "user")
public class MyUser {
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
    @Column("password")
    public String password;

    @Setter
    @Getter
    public String password_hash;
}
