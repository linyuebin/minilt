package com.daodejing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CommentDtos {

    public record CommentReq(
            /** 引用章节，null=不引用正文的随笔 */
            Integer chapterId,
            @NotBlank(message = "内容不能为空")
            @Size(max = 500, message = "内容最多500字")
            String content,
            @Size(max = 3, message = "配图最多3张")
            List<String> images,
            Long parentId,
            Long replyToUserId
    ) {}

    public record ProgressReq(@NotNull(message = "缺少章节") Integer chapterId) {}

    /** 章节评论（含嵌套回复） */
    public record CommentVO(Long id, Long userId, String nickname, String avatarChar,
                            String content, List<String> images, int likeCount, boolean liked,
                            String createdAt, List<ReplyVO> replies) {}

    public record ReplyVO(Long id, Long userId, String nickname, String avatarChar,
                          String replyToNickname, String content, String createdAt) {}

    /** 个人中心：我的留言 */
    public record MyCommentVO(Long id, Integer chapterId, String chapterTitle,
                              String content, List<String> images,
                              int likeCount, int replyCount, String createdAt) {}

    /** 个人中心：历史回复 */
    public record MyReplyVO(Long id, Integer chapterId, String chapterTitle,
                            String targetNickname, String targetContent,
                            String content, List<String> images, String createdAt) {}

    /** 个人中心：我的点赞 */
    public record MyLikeVO(Long commentId, Integer chapterId, String chapterTitle,
                           String authorNickname, String content, List<String> images,
                           int likeCount, String createdAt) {}

    /** 社区广场：所有道友的顶级感悟（chapterId 为 null 表示随笔） */
    public record FeedVO(Long id, Long userId, String nickname, String avatarChar,
                         Integer chapterId, String chapterTitle, String chapterExcerpt,
                         String content, List<String> images,
                         int likeCount, boolean liked, int replyCount, String createdAt) {}
}
