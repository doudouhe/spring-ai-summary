package com.glmapper.ai.workflow;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chinaums.common.exception.RRException;
import com.chinaums.common.security.TripleDesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 */
@Slf4j
public class MsgSecurityTools {

    /**
     * 请求密文解密 3DES 兼容数组
     *
     * @param reqStr json串，密文包含在“params”字段里面
     * @param key    24位随机串密钥
     * @return 请求明文
     */
    public static String msgDecrypt3Des(String reqStr, String key) throws RRException {

        log.info("开始请求报文解密");
        JSONObject reqJson = JSON.parseObject(reqStr);
        String paramEncry = reqJson.getString("params");
        if (StringUtils.isBlank(paramEncry)) {
            return reqStr;
        }

        byte[] paramDecry = null;
        try {
            //解密前，先反base64
            byte[] encryBytes = Base64Utils.decode(paramEncry);
            paramDecry = TripleDesUtil.decrypt(encryBytes, key.getBytes(), null);
        } catch (Exception var7) {
            log.info("3DES密钥：{}", key);
            log.info("密文：{}", paramEncry);
            log.error("请求解密异常", var7);
            throw new RRException("453", "报文解密失败");
        }

        //解出的是base64，要转码
        String paramDecryStr = new String(Base64Utils.decode(paramDecry));
        String result = null;
        if (paramDecryStr.startsWith("{")) {
            //对象
            JSONObject paramJson = JSON.parseObject(paramDecryStr);
            reqJson.remove("params");
            reqJson.putAll(paramJson);
            result = reqJson.toString();
        } else {
            //其他
            result = paramDecryStr;
        }
        return result;
    }

    /**
     * 响应报文加密
     *
     * @param respStr json串，“code”、“msg”以外的字段加密，放进“data”字段，返回json串只包含这3个字段。
     * @param key     24位随机串密钥
     * @return
     */
    public static JSONObject msgEncrypt3Des(String respStr, String key) throws RRException {
        log.info("开始响应报文加密");
        JSONObject respJson = JSON.parseObject(respStr);
        if (respJson.size() <= 2) {
            return respJson;
        } else {
            String respCode = respJson.getString("code");
            String respInfo = respJson.getString("msg");
            respJson.remove("code");
            respJson.remove("msg");
            String toEncryStr = "";

            try {
                toEncryStr = Base64Utils.encode(respJson.toString().getBytes("UTF-8"));
            } catch (Exception var9) {
                log.error("", var9);
                 throw new RRException("455", "响应数据编码异常");
            }

            String msgEncry = "";

            try {
                byte[] encryBytes = TripleDesUtil.encrypt(toEncryStr.getBytes(), key.getBytes(), (String) null);
                msgEncry = Base64Utils.encode(encryBytes);
            } catch (Exception var8) {
                log.info("3DES key：{}", key);
                log.error("响应加密异常", var8);
                throw new  RRException("453", "响应报文加密失败");
            }

            respJson.clear();
            respJson.put("code", respCode);
            respJson.put("msg", respInfo);
            respJson.put("data", msgEncry);
            return respJson;
        }
    }
}
