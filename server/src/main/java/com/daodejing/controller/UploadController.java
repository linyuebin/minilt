package com.daodejing.controller;

import com.daodejing.common.BizException;
import com.daodejing.common.R;
import com.daodejing.config.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传：悟道留言配图等场景
 * 文件落盘到 app.upload.dir（默认 upload/），通过 /api/upload/** 静态映射访问
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    @Value("${app.upload.dir:upload}")
    private String uploadDir;

    /** 上传单张图片，返回 { url: /api/upload/xxx.jpg } */
    @PostMapping("/image")
    public R<Map<String, String>> image(@RequestParam("file") MultipartFile file) {
        AuthContext.userId(); // 登录后才可上传
        if (file == null || file.isEmpty()) throw new BizException("请选择图片");
        if (file.getSize() > MAX_SIZE) throw new BizException("图片不能超过10MB");

        String original = file.getOriginalFilename();
        String ext = original == null ? "" : original.substring(original.lastIndexOf('.')).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) throw new BizException("仅支持 jpg/png/gif/webp 图片");

        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            file.transferTo(dir.resolve(filename).toFile());
            return R.ok(Map.of("url", "/api/upload/" + filename));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("图片上传失败，请稍后再试");
        }
    }
}
