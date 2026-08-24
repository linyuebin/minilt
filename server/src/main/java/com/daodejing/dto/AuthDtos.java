package com.daodejing.dto;

import com.daodejing.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record RegisterReq(
            @NotBlank(message = "请输入用户名")
            @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]{2,20}$", message = "用户名为2-20位字母/数字/下划线/中文")
            String username,
            @NotBlank(message = "请输入密码")
            @Size(min = 6, max = 32, message = "密码长度为6-32位")
            String password,
            String nickname
    ) {}

    public record LoginReq(
            @NotBlank(message = "请输入用户名") String username,
            @NotBlank(message = "请输入密码") String password
    ) {}

    public record WxLoginReq(
            @NotBlank(message = "缺少微信登录凭证") String code,
            String nickname
    ) {}

    public record AdminLoginReq(
            @NotBlank(message = "请输入用户名") String username,
            @NotBlank(message = "请输入密码") String password
    ) {}

    public record AuthResp(String token, User user) {}
}
