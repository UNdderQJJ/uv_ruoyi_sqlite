package com.ruoyi.business.domain.config;

import java.util.List;

/**
 * HTTP配置
 * 
 * @author ruoyi
 */
public class HttpSourceConfig extends SourceConfig {
    
    /** 请求URL */
    private String url;
    
    /** 请求方法 */
    private String method;
    
    /** 请求头 */
    private List<HttpHeader> headers;
    
    /** 请求体 */
    private String body;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<HttpHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(List<HttpHeader> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

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
