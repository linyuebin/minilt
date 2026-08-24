package com.daodejing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("chapter")
public class Chapter {
    @TableId(type = IdType.INPUT)
    private Integer id;
    private String title;
    private String content;
    private String interpretation;
}
