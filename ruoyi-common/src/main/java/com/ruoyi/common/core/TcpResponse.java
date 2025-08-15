package com.ruoyi.common.core;

import com.ruoyi.common.core.domain.AjaxResult;

import java.io.Serializable;

/**
 * TCP 通信响应体模型
 * 它直接包装了 RuoYi 的 AjaxResult，以保持一致性。
 */
public class TcpResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    // AjaxResult 已经包含了 code, msg, data 等字段
    private AjaxResult result;

    public TcpResponse(AjaxResult result) {
        this.result = result;
    }

    public static TcpResponse success(Object data) {
        return new TcpResponse(AjaxResult.success("操作成功", data));
    }

    public static TcpResponse error(String msg) {
        return new TcpResponse(AjaxResult.error(msg));
    }

    // --- Getter 和 Setter ---
    public AjaxResult getResult() {
        return result;
    }

    public void setResult(AjaxResult result) {
        this.result = result;
    }
}