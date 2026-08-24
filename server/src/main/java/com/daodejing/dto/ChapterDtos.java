package com.daodejing.dto;

public class ChapterDtos {

    /** 章节列表项（导航用，不含全文释义） */
    public record ChapterListItem(Integer id, String title, String preview) {}

    /** 章节详情 + 当前用户收藏状态 */
    public record ChapterDetail(Integer id, String title, String content,
                                String interpretation, boolean favorited) {}

    /** 收藏列表项 */
    public record FavoriteItem(Integer chapterId, String title, String preview, String createdAt) {}
}
