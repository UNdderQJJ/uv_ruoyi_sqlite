package com.ruoyi.framework.security.handle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 退出处理（TCP模式下不输出HTTP响应，只记录日志）
 */
@Component
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler
{
    private static final Logger log = LoggerFactory.getLogger(LogoutSuccessHandlerImpl.class);

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException
    {
        log.info("[Security] 用户登出: {} {}", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
