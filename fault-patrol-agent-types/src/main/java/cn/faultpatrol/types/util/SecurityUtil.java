package cn.faultpatrol.types.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * 安全工具：HMAC 签名校验与告警指纹
 */
public final class SecurityUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private SecurityUtil() {
    }

    /**
     * 计算请求体签名（HMAC-SHA256，小写十六进制）
     */
    public static String sign(String secret, String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 签名计算失败", e);
        }
    }

    /**
     * 常量时间比较签名，避免时序攻击
     */
    public static boolean verifySignature(String secret, String body, String providedSignature) {
        if (secret == null || secret.isEmpty() || providedSignature == null) {
            return false;
        }
        String expected = sign(secret, body);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                providedSignature.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 告警指纹（SHA-256）：用于相同告警去重合并
     */
    public static String fingerprint(String alertName, String severity, String alertContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = (alertName == null ? "" : alertName.trim()) + "|"
                    + (severity == null ? "" : severity.trim()) + "|"
                    + (alertContent == null ? "" : alertContent.trim());
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
