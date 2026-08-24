package com.daodejing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daodejing.common.BizException;
import com.daodejing.common.R;
import com.daodejing.config.AuthContext;
import com.daodejing.dto.ChapterDtos.FavoriteItem;
import com.daodejing.entity.Chapter;
import com.daodejing.entity.Favorite;
import com.daodejing.mapper.ChapterMapper;
import com.daodejing.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FavoriteMapper favoriteMapper;
    private final ChapterMapper chapterMapper;

    /** 修道藏经列表 */
    @GetMapping
    public R<List<FavoriteItem>> list() {
        long userId = AuthContext.userId();
        List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreatedAt));
        if (favorites.isEmpty()) return R.ok(List.of());

        Map<Integer, Chapter> chapters = chapterMapper.selectBatchIds(
                        favorites.stream().map(Favorite::getChapterId).toList()).stream()
                .collect(Collectors.toMap(Chapter::getId, c -> c));

        List<FavoriteItem> list = favorites.stream()
                .filter(f -> chapters.containsKey(f.getChapterId()))
                .map(f -> {
                    Chapter c = chapters.get(f.getChapterId());
                    String preview = c.getContent().length() <= 35
                            ? c.getContent() : c.getContent().substring(0, 35);
                    return new FavoriteItem(c.getId(), c.getTitle(), preview,
                            f.getCreatedAt().format(TIME_FMT));
                }).toList();
        return R.ok(list);
    }

    /** 收藏章节 */
    @PostMapping
    public R<Void> add(@RequestBody Map<String, Integer> body) {
        long userId = AuthContext.userId();
        Integer chapterId = body.get("chapterId");
        if (chapterId == null || chapterMapper.selectById(chapterId) == null) {
            throw new BizException("章节不存在");
        }
        Long exists = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getChapterId, chapterId));
        if (exists == 0) {
            Favorite f = new Favorite();
            f.setUserId(userId);
            f.setChapterId(chapterId);
            favoriteMapper.insert(f);
        }
        return R.ok();
    }

    /** 取消收藏 */
    @DeleteMapping("/{chapterId}")
    public R<Void> remove(@PathVariable int chapterId) {
        long userId = AuthContext.userId();
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getChapterId, chapterId));
        return R.ok();
    }
}
