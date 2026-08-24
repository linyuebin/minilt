package com.daodejing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.daodejing.dto.DailyCount;
import com.daodejing.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS count
            FROM `user`
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} - 1 DAY)
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            ORDER BY date
            """)
    List<DailyCount> countDailyNewUsers(@Param("days") int days);
}
