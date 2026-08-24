package com.daodejing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.daodejing.dto.ChapterHot;
import com.daodejing.entity.Chapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChapterMapper extends BaseMapper<Chapter> {

    /** 章节热度榜：评论数 + 收藏数 */
    @Select("""
            SELECT ch.id AS chapterId, ch.title AS title,
                   (SELECT COUNT(*) FROM comment c WHERE c.chapter_id = ch.id AND c.status = 0) AS commentCount,
                   (SELECT COUNT(*) FROM favorite f WHERE f.chapter_id = ch.id) AS favoriteCount
            FROM chapter ch
            ORDER BY commentCount + favoriteCount DESC, ch.id
            LIMIT #{limit}
            """)
    List<ChapterHot> selectHotChapters(@Param("limit") int limit);
}
