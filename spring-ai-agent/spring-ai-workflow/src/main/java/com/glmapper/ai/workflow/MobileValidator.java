package com.glmapper.ai.workflow;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;

public class MobileValidator implements Validator{

    private String message;

    public boolean validate(String str) {
        if(StringUtils.isEmpty(str)){
            this.message ="手机号码为空";
            return false;
        }
        if (str.length() != 11){
            this.message = "手机号码必须为11位";
            return false;
        }
        Matcher m = NUMBERIC.matcher(str);
        if (!m.matches()){
            this.message = "手机号码必须全部由数字组成";
            return false;
        }
        return true;
    }

    public String getMessage() {
        return message;
    }
}

