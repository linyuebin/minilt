package com.daodejing.dto;

import lombok.Data;

/** 章节热度（MyBatis 投影） */
@Data
public class ChapterHot {
    private Integer chapterId;
    private String title;
    private Long commentCount;
    private Long favoriteCount;
}
