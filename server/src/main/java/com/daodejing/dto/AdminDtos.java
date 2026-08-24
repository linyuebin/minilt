package com.daodejing.dto;

import java.util.List;

public class AdminDtos {

    /** 评论管理列表项 */
    public record AdminCommentVO(Long id, Integer chapterId, String chapterTitle,
                                 Long userId, String nickname, String content,
                                 Long parentId, String replyToNickname,
                                 int likeCount, int status, String createdAt) {}

    /** 用户管理列表项 */
    public record AdminUserVO(Long id, String nickname, String username, String source,
                              Integer lastChapterId, long commentCount, long favoriteCount,
                              String createdAt, String lastLoginAt) {}

    /** 统计总览 */
    public record StatsOverview(long totalUsers, long todayUsers,
                                long totalComments, long todayComments,
                                long totalFavorites) {}

    /** 趋势数据 */
    public record TrendResp(List<String> dates, List<Long> newUsers, List<Long> newComments) {}
}
