package com.daodejing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daodejing.common.BizException;
import com.daodejing.config.AuthContext;
import com.daodejing.dto.CommentDtos.*;
import com.daodejing.entity.Chapter;
import com.daodejing.entity.Comment;
import com.daodejing.entity.CommentLike;
import com.daodejing.entity.User;
import com.daodejing.mapper.ChapterMapper;
import com.daodejing.mapper.CommentLikeMapper;
import com.daodejing.mapper.CommentMapper;
import com.daodejing.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;
    private final ChapterMapper chapterMapper;

    /** 章节评论列表（顶级 + 嵌套回复），游客可看，登录用户附带点赞状态 */
    public List<CommentVO> listByChapter(int chapterId) {
        List<Comment> all = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getChapterId, chapterId)
                .eq(Comment::getStatus, 0)
                .orderByAsc(Comment::getCreatedAt));
        if (all.isEmpty()) return List.of();

        Map<Long, User> users = loadUsers(all.stream().map(Comment::getUserId).collect(Collectors.toSet()));
        Set<Long> likedIds = loadMyLikedIds(all);

        Map<Long, List<Comment>> replyMap = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId));

        return all.stream()
                .filter(c -> c.getParentId() == null)
                .sorted(Comparator.comparing(Comment::getCreatedAt).reversed())
                .map(c -> toVO(c, users, likedIds, replyMap.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    @Transactional
    public CommentVO create(CommentReq req) {
        long userId = AuthContext.userId();
        Chapter chapter = chapterMapper.selectById(req.chapterId());
        if (chapter == null) throw new BizException("章节不存在");

        Comment comment = new Comment();
        comment.setChapterId(req.chapterId());
        comment.setUserId(userId);
        comment.setContent(req.content().trim());

        if (req.parentId() != null) {
            Comment parent = commentMapper.selectById(req.parentId());
            if (parent == null || parent.getStatus() != 0) throw new BizException("原留言不存在");
            if (parent.getParentId() != null) throw new BizException("只能回复顶级留言");
            if (!parent.getChapterId().equals(req.chapterId())) throw new BizException("章节不匹配");
            comment.setParentId(parent.getId());
            comment.setReplyToUserId(req.replyToUserId() != null ? req.replyToUserId() : parent.getUserId());
        }
        commentMapper.insert(comment);

        Comment saved = commentMapper.selectById(comment.getId());
        Map<Long, User> users = loadUsers(collectUserIds(saved));
        return toVO(saved, users, Set.of(), List.of());
    }

    /** 点赞/取消点赞（toggle），返回最新点赞数与状态 */
    @Transactional
    public Map<String, Object> toggleLike(long commentId) {
        long userId = AuthContext.userId();
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 0) throw new BizException("留言不存在");

        CommentLike existing = commentLikeMapper.selectOne(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getUserId, userId));
        boolean liked;
        if (existing == null) {
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            commentLikeMapper.insert(like);
            commentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId, commentId)
                    .setSql("like_count = like_count + 1"));
            liked = true;
        } else {
            commentLikeMapper.deleteById(existing.getId());
            commentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId, commentId)
                    .setSql("like_count = GREATEST(like_count - 1, 0)"));
            liked = false;
        }
        Comment fresh = commentMapper.selectById(commentId);
        return Map.of("liked", liked, "likeCount", fresh.getLikeCount());
    }

    /** 个人中心：我的留言（顶级） */
    public List<MyCommentVO> myComments() {
        long userId = AuthContext.userId();
        List<Comment> mine = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .isNull(Comment::getParentId)
                .eq(Comment::getStatus, 0)
                .orderByDesc(Comment::getCreatedAt));
        if (mine.isEmpty()) return List.of();

        Map<Integer, String> titles = chapterTitles(mine.stream().map(Comment::getChapterId).collect(Collectors.toSet()));
        return mine.stream().map(c -> {
            Long replyCount = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getParentId, c.getId()).eq(Comment::getStatus, 0));
            return new MyCommentVO(c.getId(), c.getChapterId(),
                    titles.getOrDefault(c.getChapterId(), ""),
                    c.getContent(), c.getLikeCount(), replyCount.intValue(),
                    c.getCreatedAt().format(TIME_FMT));
        }).toList();
    }

    /** 个人中心：历史回复 */
    public List<MyReplyVO> myReplies() {
        long userId = AuthContext.userId();
        List<Comment> mine = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .isNotNull(Comment::getParentId)
                .eq(Comment::getStatus, 0)
                .orderByDesc(Comment::getCreatedAt));
        if (mine.isEmpty()) return List.of();

        Set<Long> parentIds = mine.stream().map(Comment::getParentId).collect(Collectors.toSet());
        Map<Long, Comment> parents = commentMapper.selectBatchIds(parentIds).stream()
                .collect(Collectors.toMap(Comment::getId, c -> c));
        Map<Long, User> users = loadUsers(parents.values().stream().map(Comment::getUserId).collect(Collectors.toSet()));
        Map<Integer, String> titles = chapterTitles(mine.stream().map(Comment::getChapterId).collect(Collectors.toSet()));

        return mine.stream().map(c -> {
            Comment parent = parents.get(c.getParentId());
            User target = parent == null ? null : users.get(parent.getUserId());
            return new MyReplyVO(c.getId(), c.getChapterId(),
                    titles.getOrDefault(c.getChapterId(), ""),
                    target == null ? "" : target.getNickname(),
                    parent == null ? "" : parent.getContent(),
                    c.getContent(), c.getCreatedAt().format(TIME_FMT));
        }).toList();
    }

    /** 个人中心：我的点赞 */
    public List<MyLikeVO> myLikes() {
        long userId = AuthContext.userId();
        List<CommentLike> likes = commentLikeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getUserId, userId)
                .orderByDesc(CommentLike::getCreatedAt));
        if (likes.isEmpty()) return List.of();

        Map<Long, Comment> comments = commentMapper.selectBatchIds(
                        likes.stream().map(CommentLike::getCommentId).toList()).stream()
                .filter(c -> c.getStatus() == 0)
                .collect(Collectors.toMap(Comment::getId, c -> c));
        Map<Long, User> users = loadUsers(comments.values().stream().map(Comment::getUserId).collect(Collectors.toSet()));
        Map<Integer, String> titles = chapterTitles(comments.values().stream().map(Comment::getChapterId).collect(Collectors.toSet()));

        return likes.stream()
                .map(l -> comments.get(l.getCommentId()))
                .filter(Objects::nonNull)
                .map(c -> {
                    User author = users.get(c.getUserId());
                    return new MyLikeVO(c.getId(), c.getChapterId(),
                            titles.getOrDefault(c.getChapterId(), ""),
                            author == null ? "" : author.getNickname(),
                            c.getContent(), c.getCreatedAt().format(TIME_FMT));
                }).toList();
    }

    // ===== helpers =====

    private CommentVO toVO(Comment c, Map<Long, User> users, Set<Long> likedIds, List<Comment> replies) {
        User author = users.get(c.getUserId());
        List<ReplyVO> replyVOs = replies.stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .map(r -> {
                    User rUser = users.get(r.getUserId());
                    User toUser = r.getReplyToUserId() == null ? null : users.get(r.getReplyToUserId());
                    return new ReplyVO(r.getId(), r.getUserId(),
                            rUser == null ? "道友" : rUser.getNickname(),
                            rUser == null ? "道" : rUser.getAvatarChar(),
                            toUser == null ? null : toUser.getNickname(),
                            r.getContent(), r.getCreatedAt().format(TIME_FMT));
                }).toList();
        return new CommentVO(c.getId(), c.getUserId(),
                author == null ? "道友" : author.getNickname(),
                author == null ? "道" : author.getAvatarChar(),
                c.getContent(), c.getLikeCount(),
                likedIds.contains(c.getId()),
                c.getCreatedAt().format(TIME_FMT), replyVOs);
    }

    private Map<Long, User> loadUsers(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private Set<Long> collectUserIds(Comment c) {
        Set<Long> ids = new HashSet<>();
        ids.add(c.getUserId());
        if (c.getReplyToUserId() != null) ids.add(c.getReplyToUserId());
        return ids;
    }

    private Set<Long> loadMyLikedIds(List<Comment> comments) {
        Long userId = AuthContext.userIdOrNull();
        if (userId == null) return Set.of();
        List<Long> ids = comments.stream().map(Comment::getId).toList();
        return commentLikeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                        .eq(CommentLike::getUserId, userId)
                        .in(CommentLike::getCommentId, ids)).stream()
                .map(CommentLike::getCommentId)
                .collect(Collectors.toSet());
    }

    private Map<Integer, String> chapterTitles(Set<Integer> ids) {
        if (ids.isEmpty()) return Map.of();
        return chapterMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Chapter::getId, Chapter::getTitle));
    }
}
