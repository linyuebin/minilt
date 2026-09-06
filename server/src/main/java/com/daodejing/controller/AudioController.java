package com.daodejing.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.daodejing.common.BizException;
import com.daodejing.common.R;
import com.daodejing.dto.AudioDtos;
import com.daodejing.entity.ChapterAudio;
import com.daodejing.mapper.ChapterAudioMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 经文诵读音频：音色列表 + 分章音频流（支持 Range 断点/拖动播放）
 */
@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
public class AudioController {

    private static final Pattern RANGE_PATTERN = Pattern.compile("^bytes=(\\d*)-(\\d*)$");

    private final ChapterAudioMapper chapterAudioMapper;

    /** 可用音色及入库章节数 */
    @GetMapping("/voices")
    public R<List<AudioDtos.AudioVoiceVO>> voices() {
        List<AudioDtos.AudioVoiceVO> list = chapterAudioMapper.selectMaps(Wrappers.<ChapterAudio>query()
                        .select("voice_key", "voice_name", "COUNT(*) AS cnt")
                        .groupBy("voice_key", "voice_name")
                        .orderByAsc("voice_key"))
                .stream()
                .map(row -> new AudioDtos.AudioVoiceVO(
                        String.valueOf(row.get("voice_key")),
                        String.valueOf(row.get("voice_name")),
                        ((Number) row.get("cnt")).intValue()))
                .toList();
        return R.ok(list);
    }

    /** 分章音频：GET /api/audio/xiaoxiao/1（支持 Range 206，便于拖动与边下边播） */
    @GetMapping("/{voiceKey}/{chapterId}")
    public ResponseEntity<byte[]> audio(@PathVariable String voiceKey,
                                        @PathVariable Integer chapterId,
                                        @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {
        ChapterAudio row = chapterAudioMapper.selectOne(new QueryWrapper<ChapterAudio>()
                .eq("voice_key", voiceKey)
                .eq("chapter_id", chapterId));
        if (row == null) {
            throw new BizException("该章节音频不存在");
        }
        byte[] data = row.getAudio();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename*=UTF-8''" + urlEncode(row.getFileName()));
        headers.setCacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)).cachePublic());

        Matcher rm = range == null ? null : RANGE_PATTERN.matcher(range.trim());
        if (rm != null && rm.matches()) {
            long start = rm.group(1).isEmpty() ? 0 : Long.parseLong(rm.group(1));
            long end = rm.group(2).isEmpty() ? data.length - 1 : Long.parseLong(rm.group(2));
            if (start > end || start >= data.length) {
                headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + data.length);
                return new ResponseEntity<>(headers, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
            }
            end = Math.min(end, data.length - 1);
            byte[] slice = new byte[(int) (end - start + 1)];
            System.arraycopy(data, (int) start, slice, 0, slice.length);
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + data.length);
            return new ResponseEntity<>(slice, headers, HttpStatus.PARTIAL_CONTENT);
        }
        headers.setContentLength(data.length);
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    private String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }
}
