package com.daodejing.runner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.daodejing.entity.ChapterAudio;
import com.daodejing.mapper.ChapterAudioMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 启动时把 voice/ 目录的分章诵读音频导入 chapter_audio 表。
 * 按音色增量导入：某音色在库内无数据才导入该目录，已有则以库为准。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AudioImportRunner implements ApplicationRunner {

    /** 音色标识 -> 名称；目录按名称模糊匹配（如 晓晓分章/、云夏分章/） */
    private static final Map<String, String> VOICES = Map.of(
            "xiaoxiao", "晓晓",
            "xiaoyi", "晓伊",
            "xiaoxuan", "晓萱",
            "xiaoyu", "曉雨",
            "yunxia", "云夏",
            "yunjian", "云健",
            "yunxi", "云希",
            "yunyang", "云扬");
    /** 文件名形如 001_第一章.mp3，取前导数字作为章节号 */
    private static final Pattern CHAPTER_PREFIX = Pattern.compile("^(\\d{1,3})");

    private final ChapterAudioMapper chapterAudioMapper;

    @Value("${app.audio.import-dir:../voice}")
    private String importDir;

    @Override
    public void run(ApplicationArguments args) {
        Path root = Paths.get(importDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            log.warn("音频导入目录不存在：{}，跳过导入", root);
            return;
        }
        int total = 0;
        try (Stream<Path> dirs = Files.list(root)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                String voiceKey = matchVoice(dir.getFileName().toString());
                if (voiceKey == null) {
                    continue;
                }
                // 增量：该音色已入库则跳过
                Long existing = chapterAudioMapper.selectCount(
                        new QueryWrapper<ChapterAudio>().eq("voice_key", voiceKey));
                if (existing != null && existing > 0) {
                    log.info("音色[{}] 已入库 {} 条，跳过", VOICES.get(voiceKey), existing);
                    continue;
                }
                total += importVoiceDir(dir, voiceKey);
            }
        } catch (IOException e) {
            log.error("扫描音频目录失败：{}", root, e);
            return;
        }
        if (total > 0) {
            log.info("诵读音频增量导入完成：{} 条", total);
        }
    }

    private String matchVoice(String dirName) {
        for (var entry : VOICES.entrySet()) {
            if (dirName.contains(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private int importVoiceDir(Path dir, String voiceKey) {
        String voiceName = VOICES.get(voiceKey);
        int count = 0;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(p -> p.toString().toLowerCase().endsWith(".mp3")).toList()) {
                Integer chapterId = parseChapter(file.getFileName().toString());
                if (chapterId == null || chapterId < 1 || chapterId > 81) {
                    log.warn("无法识别章节号，跳过：{}", file);
                    continue;
                }
                try {
                    byte[] data = Files.readAllBytes(file);
                    ChapterAudio row = new ChapterAudio();
                    row.setVoiceKey(voiceKey);
                    row.setVoiceName(voiceName);
                    row.setChapterId(chapterId);
                    row.setFileName(file.getFileName().toString());
                    row.setAudio(data);
                    row.setSizeBytes(data.length);
                    chapterAudioMapper.insert(row);
                    count++;
                } catch (Exception e) {
                    log.error("导入失败，跳过：{}", file, e);
                }
            }
        } catch (IOException e) {
            log.error("读取音频目录失败：{}", dir, e);
        }
        log.info("音色[{}] 导入 {} 条", voiceName, count);
        return count;
    }

    private Integer parseChapter(String fileName) {
        Matcher m = CHAPTER_PREFIX.matcher(fileName);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }
}
