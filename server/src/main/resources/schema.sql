-- 道德经阅读社区 数据库结构（幂等）
CREATE TABLE IF NOT EXISTS `user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `username`        VARCHAR(32)  NULL COMMENT 'H5账号，唯一，可空（纯微信用户）',
    `password`        VARCHAR(100) NULL COMMENT 'BCrypt密码，可空',
    `openid`          VARCHAR(64)  NULL COMMENT '微信openid，唯一，可空（纯H5用户）',
    `nickname`        VARCHAR(32)  NOT NULL DEFAULT '修道者',
    `avatar_char`     CHAR(1)      NOT NULL DEFAULT '道' COMMENT '汉字头像',
    `source`          VARCHAR(8)   NOT NULL DEFAULT 'H5' COMMENT '注册来源 H5/MP',
    `last_chapter_id` INT          NOT NULL DEFAULT 1 COMMENT '修行进度：最后阅读章节',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常 1禁用',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_login_at`   DATETIME     NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_openid` (`openid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户';

CREATE TABLE IF NOT EXISTS `chapter` (
    `id`             INT          NOT NULL COMMENT '章节号 1-81',
    `title`          VARCHAR(32)  NOT NULL,
    `content`        TEXT         NOT NULL COMMENT '经文原文',
    `interpretation` TEXT         NOT NULL COMMENT '白话释义',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='道德经章节';

CREATE TABLE IF NOT EXISTS `comment` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `chapter_id`       INT           NULL COMMENT '引用章节，NULL=不引用正文的随笔',
    `user_id`          BIGINT        NOT NULL,
    `parent_id`        BIGINT        NULL COMMENT '空=顶级留言，否则为所回复的顶级留言id',
    `reply_to_user_id` BIGINT        NULL COMMENT '被回复人',
    `content`          VARCHAR(1000) NOT NULL,
    `images`           VARCHAR(1000) NULL COMMENT '配图URL列表（JSON数组，最多3张）',
    `like_count`       INT           NOT NULL DEFAULT 0,
    `status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_chapter` (`chapter_id`, `status`, `created_at`),
    KEY `idx_user` (`user_id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='悟道留言/回复';

CREATE TABLE IF NOT EXISTS `comment_like` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `comment_id` BIGINT   NOT NULL,
    `user_id`    BIGINT   NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
    KEY `idx_user` (`user_id`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='留言点赞';

CREATE TABLE IF NOT EXISTS `favorite` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT   NOT NULL,
    `chapter_id` INT      NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_chapter` (`user_id`, `chapter_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='收藏（修道藏经）';

CREATE TABLE IF NOT EXISTS `admin_user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `username`   VARCHAR(32)  NOT NULL,
    `password`   VARCHAR(100) NOT NULL COMMENT 'BCrypt',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='管理端账号';

CREATE TABLE IF NOT EXISTS `chapter_audio` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `voice_key`  VARCHAR(32) NOT NULL COMMENT '音色标识 xiaoxiao/yunxia',
    `voice_name` VARCHAR(32) NOT NULL COMMENT '音色名称 晓晓/云夏',
    `chapter_id` INT         NOT NULL COMMENT '章节号 1-81',
    `file_name`  VARCHAR(128) NOT NULL COMMENT '原始文件名',
    `audio`      MEDIUMBLOB  NOT NULL COMMENT '音频内容（mp3，单节约170KB）',
    `size_bytes` INT         NOT NULL,
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_voice_chapter` (`voice_key`, `chapter_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='经文诵读音频（分章）';
