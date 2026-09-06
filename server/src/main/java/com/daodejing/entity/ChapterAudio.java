package com.daodejing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chapter_audio")
public class ChapterAudio {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String voiceKey;
    private String voiceName;
    private Integer chapterId;
    private String fileName;
    /** 音频内容（mp3） */
    private byte[] audio;
    private Integer sizeBytes;
    private LocalDateTime createdAt;
}
