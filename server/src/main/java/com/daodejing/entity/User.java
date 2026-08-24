package com.daodejing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {
    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    @JsonIgnore
    private String openid;
    private String nickname;
    private String avatarChar;
    private String source;
    private Integer lastChapterId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
