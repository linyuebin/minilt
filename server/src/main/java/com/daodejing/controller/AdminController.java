package com.daodejing.controller;

import com.daodejing.common.PageResult;
import com.daodejing.common.R;
import com.daodejing.config.AuthContext;
import com.daodejing.dto.AdminDtos.*;
import com.daodejing.dto.AuthDtos.AdminLoginReq;
import com.daodejing.dto.ChapterHot;
import com.daodejing.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/auth/login")
    public R<Map<String, Object>> login(@Valid @RequestBody AdminLoginReq req) {
        return R.ok(adminService.login(req.username(), req.password()));
    }

    @GetMapping("/comments")
    public R<PageResult<AdminCommentVO>> comments(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(required = false) Integer chapterId,
                                                  @RequestParam(required = false) String keyword) {
        AuthContext.adminId();
        return R.ok(adminService.pageComments(page, size, chapterId, keyword));
    }

    @DeleteMapping("/comments/{id}")
    public R<Void> deleteComment(@PathVariable long id) {
        AuthContext.adminId();
        adminService.deleteComment(id);
        return R.ok();
    }

    @GetMapping("/users")
    public R<PageResult<AdminUserVO>> users(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @RequestParam(required = false) String source,
                                            @RequestParam(required = false) String keyword) {
        AuthContext.adminId();
        return R.ok(adminService.pageUsers(page, size, source, keyword));
    }

    @GetMapping("/stats/overview")
    public R<StatsOverview> overview() {
        AuthContext.adminId();
        return R.ok(adminService.statsOverview());
    }

    @GetMapping("/stats/trend")
    public R<TrendResp> trend(@RequestParam(defaultValue = "30") int days) {
        AuthContext.adminId();
        return R.ok(adminService.statsTrend(Math.min(Math.max(days, 7), 90)));
    }

    @GetMapping("/stats/chapters")
    public R<List<ChapterHot>> hotChapters(@RequestParam(defaultValue = "10") int limit) {
        AuthContext.adminId();
        return R.ok(adminService.statsHotChapters(Math.min(Math.max(limit, 1), 81)));
    }
}
