package com.daodejing.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtUtil {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";

    private final SecretKey key;
    private final Duration expire;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-days}") int expireDays) {
        this.key = new SecretKeySpec(normalize(secret), "HmacSHA256");
        this.expire = Duration.ofDays(expireDays);
    }

    /** HS256 密钥至少 32 字节，不足则补零 */
    private static byte[] normalize(String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) return raw;
        byte[] padded = new byte[32];
        System.arraycopy(raw, 0, padded, 0, raw.length);
        return padded;
    }

    public String createToken(long id, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire.toMillis()))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 token，无效或过期返回 null
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
