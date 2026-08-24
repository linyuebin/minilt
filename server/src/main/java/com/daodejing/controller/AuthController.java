package com.daodejing.controller;

import com.daodejing.common.R;
import com.daodejing.dto.AuthDtos.*;
import com.daodejing.entity.User;
import com.daodejing.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public R<AuthResp> register(@Valid @RequestBody RegisterReq req) {
        return R.ok(authService.register(req));
    }

    @PostMapping("/login")
    public R<AuthResp> login(@Valid @RequestBody LoginReq req) {
        return R.ok(authService.login(req));
    }

    @PostMapping("/wx-login")
    public R<AuthResp> wxLogin(@Valid @RequestBody WxLoginReq req) {
        return R.ok(authService.wxLogin(req));
    }

    @GetMapping("/me")
    public R<User> me() {
        return R.ok(authService.me());
    }
}
