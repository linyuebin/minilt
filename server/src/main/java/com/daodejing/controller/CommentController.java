package com.daodejing.controller;

import com.daodejing.common.R;
import com.daodejing.dto.CommentDtos.CommentReq;
import com.daodejing.dto.CommentDtos.CommentVO;
import com.daodejing.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** 发表留言/回复（parentId 区分） */
    @PostMapping
    public R<CommentVO> create(@Valid @RequestBody CommentReq req) {
        return R.ok(commentService.create(req));
    }

    /** 点赞/取消点赞 */
    @PostMapping("/{id}/like")
    public R<Map<String, Object>> toggleLike(@PathVariable long id) {
        return R.ok(commentService.toggleLike(id));
    }
}
