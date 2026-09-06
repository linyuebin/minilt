package com.daodejing.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "comment", autoResultMap = true)
public class Comment {
    private Long id;
    private Integer chapterId;
    private Long userId;
    private Long parentId;
    private Long replyToUserId;
    private String content;
    /** 配图URL列表（JSON存储，最多3张） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;
    private Integer likeCount;
    private Integer status;
    private LocalDateTime createdAt;
}
