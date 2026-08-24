package com.daodejing.dto;

import lombok.Data;

/** 按天统计（MyBatis 投影） */
@Data
public class DailyCount {
    private String date;
    private Long count;
}
