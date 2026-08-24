package com.daodejing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daodejing.common.BizException;
import com.daodejing.config.AuthContext;
import com.daodejing.config.JwtUtil;
import com.daodejing.dto.AuthDtos.*;
import com.daodejing.entity.User;
import com.daodejing.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String AVATAR_CHARS = "道德经无为悟玄";

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${app.wx.mock}")
    private boolean wxMock;
    @Value("${app.wx.appid}")
    private String wxAppid;
    @Value("${app.wx.secret}")
    private String wxSecret;

    public AuthResp register(RegisterReq req) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.username()));
        if (exists > 0) throw new BizException("用户名已被占用");

        User user = new User();
        user.setUsername(req.username());
        user.setPassword(encoder.encode(req.password()));
        user.setNickname(blankToDefault(req.nickname(), req.username()));
        user.setAvatarChar(randomAvatarChar());
        user.setSource("H5");
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.insert(user);
        return new AuthResp(jwtUtil.createToken(user.getId(), JwtUtil.ROLE_USER), user);
    }

    public AuthResp login(LoginReq req) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.username()));
        if (user == null || user.getPassword() == null
                || !encoder.matches(req.password(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        checkStatus(user);
        touchLogin(user);
        return new AuthResp(jwtUtil.createToken(user.getId(), JwtUtil.ROLE_USER), user);
    }

    public AuthResp wxLogin(WxLoginReq req) {
        String openid = resolveOpenid(req.code());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid));
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(blankToDefault(req.nickname(), "修道者"));
            user.setAvatarChar(randomAvatarChar());
            user.setSource("MP");
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.insert(user);
        } else {
            checkStatus(user);
            touchLogin(user);
        }
        return new AuthResp(jwtUtil.createToken(user.getId(), JwtUtil.ROLE_USER), user);
    }

    public User me() {
        User user = userMapper.selectById(AuthContext.userId());
        if (user == null) throw new BizException(401, "请先登录");
        return user;
    }

    private String resolveOpenid(String code) {
        if (wxMock) {
            // 开发期：code 直接作为 openid，便于本地/开发者工具联调
            return "mock-" + code;
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + wxAppid
                + "&secret=" + wxSecret + "&js_code=" + code + "&grant_type=authorization_code";
        try {
            String body = RestClient.create().get().uri(url).retrieve().body(String.class);
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("openid")) return node.get("openid").asText();
            log.warn("wx jscode2session failed: {}", body);
            throw new BizException("微信登录失败：" + node.path("errmsg").asText("未知错误"));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("wx jscode2session error", e);
            throw new BizException("微信登录服务暂不可用");
        }
    }

    private void checkStatus(User user) {
        if (user.getStatus() != null && user.getStatus() != 0) {
            throw new BizException(403, "账号已被禁用");
        }
    }

    private void touchLogin(User user) {
        User update = new User();
        update.setId(user.getId());
        update.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(update);
    }

    private static String blankToDefault(String v, String def) {
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    private static String randomAvatarChar() {
        return String.valueOf(AVATAR_CHARS.charAt(
                ThreadLocalRandom.current().nextInt(AVATAR_CHARS.length())));
    }
}
