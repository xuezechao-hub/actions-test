package com.huawei.apm.core.lubanops.integration.access;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.huawei.apm.core.lubanops.integration.utils.StringUtil;

/**
 * 实现hmac的签名算法的util类
 * @author
 * @since 2020/4/7
 **/
public class HMacSignatureUtil {
    /**
     * hmac+签名算法 加密
     * @param content
     *            内容
     * @param key
     *            加密秘钥
     * @param hMacAlgorithm
     *            hamc签名算法名称:例如HmacMD5,HmacSHA1,HmacSHA256
     * @return 签名后的数据，并且base64算法转成可见字符
     */
    public static String getHmacSign(String content, String key, HMacAlgorithm hMacAlgorithm) {

        try {
            // 根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
            SecretKeySpec signinKey = new SecretKeySpec(key.getBytes(), hMacAlgorithm.name());
            // 生成一个指定 Mac 算法 的 Mac 对象
            Mac mac = Mac.getInstance(hMacAlgorithm.name());
            // 用给定密钥初始化 Mac 对象
            mac.init(signinKey);
            // 完成 Mac 操作
            byte[] rawHmac;
            rawHmac = mac.doFinal(content.getBytes(Charset.forName("UTF-8")));

            return StringUtil.bytesToString(rawHmac);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("NoSuchAlgorithmException", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("InvalidKeyException", e);
        } catch (IllegalStateException e) {
            throw new RuntimeException("IllegalStateException", e);
        }

    }
}
