package com.ruoyi.framework.web.service;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import eu.bitwalker.useragentutils.UserAgent;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * token验证处理（兼容TCP模式）
 */
@Component
public class TokenService
{
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    @Value("${token.header}")
    private String header;

    @Value("${token.secret}")
    private String secret;

    @Value("${token.expireTime}")
    private int expireTime;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private static final Long MILLIS_MINUTE_TEN = 20 * 60 * 1000L;

    @Autowired(required = false)
    private RedisCache redisCache;

    public LoginUser getLoginUser(HttpServletRequest request)
    {
        try
        {
            String token = null;
            if (request != null)
            {
                token = getToken(request);
            }
            if (StringUtils.isNotEmpty(token))
            {
                Claims claims = parseToken(token);
                String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
                String userKey = getTokenKey(uuid);
                return redisCache != null ? redisCache.getCacheObject(userKey) : null;
            }
        }
        catch (Exception e)
        {
            log.error("获取用户信息异常'{}'", e.getMessage(), e);
        }
        return null;
    }

    public void setLoginUser(LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            refreshToken(loginUser);
        }
    }

    public void delLoginUser(String token)
    {
        if (StringUtils.isNotEmpty(token) && redisCache != null)
        {
            String userKey = getTokenKey(token);
            redisCache.deleteObject(userKey);
        }
    }

    public String createToken(LoginUser loginUser)
    {
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        setUserAgent(loginUser);
        refreshToken(loginUser);
        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.LOGIN_USER_KEY, token);
        return createToken(claims);
    }

    public void refreshToken(LoginUser loginUser)
    {
        long expireTimeMs = loginUser.getLoginTime() + expireTime * MILLIS_MINUTE;
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(expireTimeMs);
        String userKey = getTokenKey(loginUser.getToken());
        if (redisCache != null)
        {
            redisCache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
        }
    }

    public void verifyToken(LoginUser loginUser)
    {
        long expireTimeMs = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTimeMs - currentTime <= MILLIS_MINUTE_TEN)
        {
            refreshToken(loginUser);
        }
    }

    public void setUserAgent(LoginUser loginUser)
    {
        // TCP模式无请求头，填充默认信息
        loginUser.setIpaddr(IpUtils.getIpAddr());
        loginUser.setLoginLocation("TCP");
        loginUser.setBrowser("TCP");
        loginUser.setOs("TCP");
    }

    public String getToken(HttpServletRequest request)
    {
        String token = request.getHeader(header);
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }

    private String createToken(Map<String, Object> claims)
    {
        return Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    private Claims parseToken(String token)
    {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    private String getTokenKey(String uuid)
    {
        return CacheConstants.LOGIN_TOKEN_KEY + uuid;
    }
}
