package com.bot.ping_server.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@EntityScan
@Data
@Table(name = "contact")
public class Contact {
    @Setter
    @Getter
    @Column("uuid1")
    public String uuid1;

    @Setter
    @Getter
    @Column("uuid2")
    public String uuid2;
}
