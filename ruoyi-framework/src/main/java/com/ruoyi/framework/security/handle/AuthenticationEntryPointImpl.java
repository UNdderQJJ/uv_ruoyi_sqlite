package com.ruoyi.framework.security.handle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 认证失败处理（TCP模式下不输出HTTP响应，只记录日志）
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint
{
    private static final Logger log = LoggerFactory.getLogger(AuthenticationEntryPointImpl.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException
    {
        log.warn("[Security] 未认证访问被拦截: {} {} - {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        // TCP模式下，不再通过HTTP返回体输出
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
