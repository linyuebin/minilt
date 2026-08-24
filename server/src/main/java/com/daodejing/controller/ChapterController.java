package com.daodejing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daodejing.common.BizException;
import com.daodejing.common.R;
import com.daodejing.config.AuthContext;
import com.daodejing.dto.ChapterDtos.ChapterDetail;
import com.daodejing.dto.ChapterDtos.ChapterListItem;
import com.daodejing.dto.CommentDtos.CommentVO;
import com.daodejing.entity.Chapter;
import com.daodejing.entity.Favorite;
import com.daodejing.mapper.ChapterMapper;
import com.daodejing.mapper.FavoriteMapper;
import com.daodejing.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterMapper chapterMapper;
    private final FavoriteMapper favoriteMapper;
    private final CommentService commentService;

    /** 章节列表（导航/目录用） */
    @GetMapping
    public R<List<ChapterListItem>> list() {
        List<ChapterListItem> list = chapterMapper.selectList(
                        new LambdaQueryWrapper<Chapter>().orderByAsc(Chapter::getId)).stream()
                .map(c -> new ChapterListItem(c.getId(), c.getTitle(), preview(c.getContent())))
                .toList();
        return R.ok(list);
    }

    /** 章节详情（含当前用户收藏状态） */
    @GetMapping("/{id}")
    public R<ChapterDetail> detail(@PathVariable int id) {
        Chapter c = chapterMapper.selectById(id);
        if (c == null) throw new BizException(404, "章节不存在");

        boolean favorited = false;
        Long userId = AuthContext.userIdOrNull();
        if (userId != null) {
            favorited = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, userId)
                    .eq(Favorite::getChapterId, id)) > 0;
        }
        return R.ok(new ChapterDetail(c.getId(), c.getTitle(), c.getContent(),
                c.getInterpretation(), favorited));
    }

    /** 章节评论列表 */
    @GetMapping("/{id}/comments")
    public R<List<CommentVO>> comments(@PathVariable int id) {
        return R.ok(commentService.listByChapter(id));
    }

    private static String preview(String content) {
        return content.length() <= 35 ? content : content.substring(0, 35);
    }
}
