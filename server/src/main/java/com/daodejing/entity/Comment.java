package com.daodejing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    private Long id;
    private Integer chapterId;
    private Long userId;
    private Long parentId;
    private Long replyToUserId;
    private String content;
    private Integer likeCount;
    private Integer status;
    private LocalDateTime createdAt;
}
