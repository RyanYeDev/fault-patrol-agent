package cn.faultpatrol.test.unit;

import cn.faultpatrol.types.util.SecurityUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * 安全工具单元测试（签名校验 / 告警指纹）
 */
public class SecurityUtilTest {

    @Test
    public void testFingerprintDeterministic() {
        String a = SecurityUtil.fingerprint("订单错误率告警", "critical", "5xx 突增");
        String b = SecurityUtil.fingerprint("订单错误率告警", "critical", "5xx 突增");
        assertEquals(a, b);
    }

    @Test
    public void testFingerprintDiffersByField() {
        String a = SecurityUtil.fingerprint("订单错误率告警", "critical", "5xx 突增");
        String b = SecurityUtil.fingerprint("订单错误率告警", "warning", "5xx 突增");
        String c = SecurityUtil.fingerprint("支付回调告警", "critical", "5xx 突增");
        assertNotEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    public void testFingerprintNullSafe() {
        String a = SecurityUtil.fingerprint(null, null, null);
        String b = SecurityUtil.fingerprint("", "", "");
        assertEquals(a, b);
    }

    @Test
    public void testSignAndVerify() {
        String secret = "test-secret";
        String body = "{\"alertName\":\"test\"}";
        String signature = SecurityUtil.sign(secret, body);
        assertTrue(SecurityUtil.verifySignature(secret, body, signature));
    }

    @Test
    public void testVerifyRejectsTamperedBody() {
        String secret = "test-secret";
        String signature = SecurityUtil.sign(secret, "{\"a\":1}");
        assertFalse(SecurityUtil.verifySignature(secret, "{\"a\":2}", signature));
    }

    @Test
    public void testVerifyRejectsMissingInputs() {
        assertFalse(SecurityUtil.verifySignature("", "body", "sig"));
        assertFalse(SecurityUtil.verifySignature(null, "body", "sig"));
        assertFalse(SecurityUtil.verifySignature("secret", "body", null));
    }

    @Test
    public void testVerifyCaseInsensitiveSignature() {
        String secret = "s";
        String sig = SecurityUtil.sign(secret, "body");
        assertTrue(SecurityUtil.verifySignature(secret, "body", sig.toUpperCase()));
    }

}
