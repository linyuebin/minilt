package com.daodejing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CommentDtos {

    public record CommentReq(
            @NotNull(message = "缺少章节") Integer chapterId,
            @NotBlank(message = "内容不能为空")
            @Size(max = 500, message = "内容最多500字")
            String content,
            Long parentId,
            Long replyToUserId
    ) {}

    public record ProgressReq(@NotNull(message = "缺少章节") Integer chapterId) {}

    /** 章节评论（含嵌套回复） */
    public record CommentVO(Long id, Long userId, String nickname, String avatarChar,
                            String content, int likeCount, boolean liked,
                            String createdAt, List<ReplyVO> replies) {}

    public record ReplyVO(Long id, Long userId, String nickname, String avatarChar,
                          String replyToNickname, String content, String createdAt) {}

    /** 个人中心：我的留言 */
    public record MyCommentVO(Long id, Integer chapterId, String chapterTitle,
                              String content, int likeCount, int replyCount, String createdAt) {}

    /** 个人中心：历史回复 */
    public record MyReplyVO(Long id, Integer chapterId, String chapterTitle,
                            String targetNickname, String targetContent,
                            String content, String createdAt) {}

    /** 个人中心：我的点赞 */
    public record MyLikeVO(Long commentId, Integer chapterId, String chapterTitle,
                           String authorNickname, String content, String createdAt) {}
}
