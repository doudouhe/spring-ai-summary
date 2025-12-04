package com.glmapper.ai.workflow;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;

public class NumbericValidator implements Validator {
    private String message;

    public boolean validate(String str) {
        if (StringUtils.isEmpty(str)) {
            this.message = "输入字符串为空";
            return false;
        }
        Matcher m = NUMBERIC.matcher(str);
        if (!m.matches()) {
            this.message = "输入字符串必须全部由数字组成";
            return false;
        }
        return true;
    }

    public String getMessage() {
        return message;
    }
}
