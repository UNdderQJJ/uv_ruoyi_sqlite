package com.ruoyi.business.domain.config;

import lombok.Data;

import java.util.List;

/**
 * HTTP配置
 * 
 * @author ruoyi
 */
@Data
public class HttpSourceConfig extends SourceConfig {
    
    /** 请求URL */
    private String url;
    
    /** 请求方法 */
    private String method;
    
    /** 请求头 */
    private List<HttpHeader> headers;
    
    /** 请求体 */
    private String body;


    /**
     * HTTP请求头
     */
    public static class HttpHeader {
        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
