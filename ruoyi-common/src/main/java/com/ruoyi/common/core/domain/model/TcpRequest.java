package com.ruoyi.common.core.domain.model;

import java.io.Serializable;

/**
 * TCP 通信请求体模型
 */
public class TcpRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 请求唯一标识，由前端生成并透传，服务端原样回显
     */
    private String id;

    /**
     * 请求路径，用于路由到不同的业务逻辑
     * 例如: "/system/user/list"
     */
    private String path;

    /**
     * 请求参数，使用 JSON 字符串表示
     * 例如: '{"pageNum": 1, "pageSize": 10, "userName": "admin"}'
     */
    private String body;

    // --- Getter 和 Setter ---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "TcpRequest{" +
                "id='" + id + '\'' +
                ", path='" + path + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}