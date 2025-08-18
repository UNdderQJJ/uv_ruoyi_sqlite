package com.ruoyi.common.core;

import java.io.Serializable;

/**
 * TCP响应对象
 * 
 * 通用响应结构：{ code, message, data }
 */
public class TcpResponse implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 响应码 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 响应数据（可为任意类型：Map/List/POJO/String/自定义对象等） */
    private Object result;

    public TcpResponse()
    {
    }

    public TcpResponse(int code, String message)
    {
        this.code = code;
        this.message = message;
    }

    public TcpResponse(int code, String message, Object result)
    {
        this.code = code;
        this.message = message;
        this.result = result;
    }

    // ---------- 静态工厂：成功 ----------
    public static TcpResponse success()
    {
        return new TcpResponse(200, "操作成功");
    }

    public static TcpResponse success(String message)
    {
        return new TcpResponse(200, message);
    }

    public static TcpResponse success(Object data)
    {
        return new TcpResponse(200, "操作成功", data);
    }

    public static TcpResponse success(String message, Object data)
    {
        return new TcpResponse(200, message, data);
    }

    // ---------- 静态工厂：失败 ----------
    public static TcpResponse error()
    {
        return new TcpResponse(500, "操作失败");
    }

    public static TcpResponse error(String message)
    {
        return new TcpResponse(500, message);
    }

    public static TcpResponse error(int code, String message)
    {
        return new TcpResponse(code, message);
    }

    public static TcpResponse error(String message, Object data)
    {
        return new TcpResponse(500, message, data);
    }

    // ---------- Getter / Setter ----------
    public int getCode()
    {
        return code;
    }

    public void setCode(int code)
    {
        this.code = code;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public Object getResult()
    {
        return result;
    }

    public void setResult(Object result)
    {
        this.result = result;
    }

    @Override
    public String toString()
    {
        return "TcpResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", result=" + result +
                '}';
    }
}