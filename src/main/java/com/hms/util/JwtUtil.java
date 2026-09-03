package com.hms.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility class for JWT-based stateless authentication.
 * Replaces server-side HttpSession storage to enable horizontal scaling on EKS.
 */
public class JwtUtil {

    private static final String JWT_SECRET_ENV = "JWT_SECRET";
    private static final String DEFAULT_SECRET = "DocPortalJwtSecretKeyForEKSDeployment2024!";
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L; // 24 hours
    public static final String JWT_COOKIE_NAME = "JWT_TOKEN";
    public static final String MSG_COOKIE_NAME = "APP_MSG";
    public static final String MSG_TYPE_COOKIE = "APP_MSG_TYPE";

    private static SecretKey getSigningKey() {
        String secret = System.getenv(JWT_SECRET_ENV);
        if (secret == null || secret.isEmpty()) {
            secret = DEFAULT_SECRET;
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Ensure key is at least 256 bits for HS256
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            keyBytes = paddedKey;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a JWT token for the given subject (user/doctor/admin identifier).
     */
    public static String generateToken(String subject, String role) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate and parse a JWT token, returning its Claims.
     * Returns null if the token is invalid or expired.
     */
    public static Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Set a JWT token as an HttpOnly cookie in the response.
     */
    public static void setJwtCookie(HttpServletResponse resp, String token) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (EXPIRATION_MS / 1000));
        resp.addCookie(cookie);
    }

    /**
     * Clear the JWT cookie (logout).
     */
    public static void clearJwtCookie(HttpServletResponse resp) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        resp.addCookie(cookie);
    }

    /**
     * Retrieve the JWT token string from the request cookies.
     */
    public static String getTokenFromRequest(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (JWT_COOKIE_NAME.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Set a short-lived flash message cookie (success or error).
     */
    public static void setMessageCookie(HttpServletResponse resp, String message, String type) {
        Cookie msgCookie = new Cookie(MSG_COOKIE_NAME, encodeValue(message));
        msgCookie.setPath("/");
        msgCookie.setMaxAge(30);
        resp.addCookie(msgCookie);

        Cookie typeCookie = new Cookie(MSG_TYPE_COOKIE, type);
        typeCookie.setPath("/");
        typeCookie.setMaxAge(30);
        resp.addCookie(typeCookie);
    }

    private static String encodeValue(String value) {
        if (value == null) return "";
        return value.replace(" ", "+");
    }
}
