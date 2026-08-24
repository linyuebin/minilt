package com.daodejing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.daodejing.dto.DailyCount;
import com.daodejing.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS count
            FROM comment
            WHERE status = 0
              AND created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} - 1 DAY)
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            ORDER BY date
            """)
    List<DailyCount> countDailyComments(@Param("days") int days);
}
