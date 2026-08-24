package com.daodejing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daodejing.common.BizException;
import com.daodejing.common.PageResult;
import com.daodejing.config.JwtUtil;
import com.daodejing.dto.AdminDtos.*;
import com.daodejing.dto.ChapterHot;
import com.daodejing.dto.DailyCount;
import com.daodejing.entity.*;
import com.daodejing.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AdminUserMapper adminUserMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final FavoriteMapper favoriteMapper;
    private final ChapterMapper chapterMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Map<String, Object> login(String username, String password) {
        AdminUser admin = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, username));
        if (admin == null || !encoder.matches(password, admin.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        return Map.of("token", jwtUtil.createToken(admin.getId(), JwtUtil.ROLE_ADMIN),
                "username", admin.getUsername());
    }

    /** 评论管理分页：可按章节/关键字筛选，含已删除 */
    public PageResult<AdminCommentVO> pageComments(int page, int size, Integer chapterId, String keyword) {
        LambdaQueryWrapper<Comment> qw = new LambdaQueryWrapper<Comment>()
                .eq(chapterId != null, Comment::getChapterId, chapterId)
                .like(keyword != null && !keyword.isBlank(), Comment::getContent, keyword)
                .orderByDesc(Comment::getCreatedAt);
        Page<Comment> p = commentMapper.selectPage(new Page<>(page, size), qw);
        List<Comment> records = p.getRecords();
        if (records.isEmpty()) return new PageResult<>(p.getTotal(), List.of());

        Map<Long, User> users = loadUsers(records.stream()
                .flatMap(c -> c.getReplyToUserId() == null
                        ? java.util.stream.Stream.of(c.getUserId())
                        : java.util.stream.Stream.of(c.getUserId(), c.getReplyToUserId()))
                .collect(Collectors.toSet()));
        Map<Integer, String> titles = chapterTitles(records.stream()
                .map(Comment::getChapterId).collect(Collectors.toSet()));

        List<AdminCommentVO> list = records.stream().map(c -> {
            User author = users.get(c.getUserId());
            User replyTo = c.getReplyToUserId() == null ? null : users.get(c.getReplyToUserId());
            return new AdminCommentVO(c.getId(), c.getChapterId(),
                    titles.getOrDefault(c.getChapterId(), ""),
                    c.getUserId(), author == null ? "-" : author.getNickname(),
                    c.getContent(), c.getParentId(),
                    replyTo == null ? null : replyTo.getNickname(),
                    c.getLikeCount(), c.getStatus(),
                    c.getCreatedAt().format(TIME_FMT));
        }).toList();
        return new PageResult<>(p.getTotal(), list);
    }

    /** 软删除评论；顶级留言连带其回复一起删除 */
    @Transactional
    public void deleteComment(long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw new BizException("评论不存在");
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .eq(Comment::getId, id).set(Comment::getStatus, 1));
        if (comment.getParentId() == null) {
            commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getParentId, id).set(Comment::getStatus, 1));
        }
    }

    /** 用户列表分页：附评论数/收藏数 */
    public PageResult<AdminUserVO> pageUsers(int page, int size, String source, String keyword) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
                .eq(source != null && !source.isBlank(), User::getSource, source)
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(User::getNickname, keyword).or().like(User::getUsername, keyword))
                .orderByDesc(User::getCreatedAt);
        Page<User> p = userMapper.selectPage(new Page<>(page, size), qw);

        List<AdminUserVO> list = p.getRecords().stream().map(u -> {
            long commentCount = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getUserId, u.getId()).eq(Comment::getStatus, 0));
            long favoriteCount = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, u.getId()));
            return new AdminUserVO(u.getId(), u.getNickname(), u.getUsername(), u.getSource(),
                    u.getLastChapterId(), commentCount, favoriteCount,
                    u.getCreatedAt().format(TIME_FMT),
                    u.getLastLoginAt() == null ? "-" : u.getLastLoginAt().format(TIME_FMT));
        }).toList();
        return new PageResult<>(p.getTotal(), list);
    }

    public StatsOverview statsOverview() {
        long totalUsers = userMapper.selectCount(null);
        long todayUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreatedAt, LocalDate.now().atStartOfDay()));
        long totalComments = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, 0));
        long todayComments = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, 0)
                .ge(Comment::getCreatedAt, LocalDate.now().atStartOfDay()));
        long totalFavorites = favoriteMapper.selectCount(null);
        return new StatsOverview(totalUsers, todayUsers, totalComments, todayComments, totalFavorites);
    }

    /** 近 N 天每日新增用户/评论（缺失日期补零） */
    public TrendResp statsTrend(int days) {
        Map<String, Long> userMap = toMap(userMapper.countDailyNewUsers(days));
        Map<String, Long> commentMap = toMap(commentMapper.countDailyComments(days));

        List<String> dates = new ArrayList<>();
        List<Long> newUsers = new ArrayList<>();
        List<Long> newComments = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(days - 1);
        for (int i = 0; i < days; i++) {
            String d = start.plusDays(i).format(DATE_FMT);
            dates.add(d);
            newUsers.add(userMap.getOrDefault(d, 0L));
            newComments.add(commentMap.getOrDefault(d, 0L));
        }
        return new TrendResp(dates, newUsers, newComments);
    }

    public List<ChapterHot> statsHotChapters(int limit) {
        return chapterMapper.selectHotChapters(limit);
    }

    private static Map<String, Long> toMap(List<DailyCount> list) {
        return list.stream().collect(Collectors.toMap(DailyCount::getDate, DailyCount::getCount));
    }

    private Map<Long, User> loadUsers(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private Map<Integer, String> chapterTitles(Set<Integer> ids) {
        if (ids.isEmpty()) return Map.of();
        return chapterMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Chapter::getId, Chapter::getTitle));
    }
}
