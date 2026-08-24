package com.daodejing.controller;

import com.daodejing.common.BizException;
import com.daodejing.common.R;
import com.daodejing.config.AuthContext;
import com.daodejing.dto.CommentDtos.*;
import com.daodejing.entity.User;
import com.daodejing.mapper.UserMapper;
import com.daodejing.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final CommentService commentService;

    /** 同步修行进度（最后阅读章节） */
    @PutMapping("/progress")
    public R<Void> updateProgress(@Valid @RequestBody ProgressReq req) {
        long userId = AuthContext.userId();
        if (req.chapterId() < 1 || req.chapterId() > 81) throw new BizException("章节号无效");
        User update = new User();
        update.setId(userId);
        update.setLastChapterId(req.chapterId());
        userMapper.updateById(update);
        return R.ok();
    }

    /** 个人中心：我的留言 */
    @GetMapping("/comments")
    public R<List<MyCommentVO>> myComments() {
        return R.ok(commentService.myComments());
    }

    /** 个人中心：历史回复 */
    @GetMapping("/replies")
    public R<List<MyReplyVO>> myReplies() {
        return R.ok(commentService.myReplies());
    }

    /** 个人中心：我的点赞 */
    @GetMapping("/likes")
    public R<List<MyLikeVO>> myLikes() {
        return R.ok(commentService.myLikes());
    }
}
