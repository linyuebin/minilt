package com.daodejing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_user")
public class AdminUser {
    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private LocalDateTime createdAt;
}
