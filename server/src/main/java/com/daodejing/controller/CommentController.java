package com.daodejing.controller;

import com.daodejing.common.PageResult;
import com.daodejing.common.R;
import com.daodejing.dto.CommentDtos.CommentReq;
import com.daodejing.dto.CommentDtos.CommentVO;
import com.daodejing.dto.CommentDtos.FeedVO;
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

    /** 发表留言/回复（parentId 区分；chapterId 可空=不引用正文的随笔） */
    @PostMapping
    public R<CommentVO> create(@Valid @RequestBody CommentReq req) {
        return R.ok(commentService.create(req));
    }

    /** 社区广场：所有道友的顶级感悟（filter: all/featured/dao/de，游客可看） */
    @GetMapping("/feed")
    public R<PageResult<FeedVO>> feed(@RequestParam(defaultValue = "all") String filter,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return R.ok(commentService.feed(filter, page, size));
    }

    /** 点赞/取消点赞 */
    @PostMapping("/{id}/like")
    public R<Map<String, Object>> toggleLike(@PathVariable long id) {
        return R.ok(commentService.toggleLike(id));
    }
}
