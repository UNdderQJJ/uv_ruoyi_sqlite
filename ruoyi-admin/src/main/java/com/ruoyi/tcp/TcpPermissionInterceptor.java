package com.ruoyi.tcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * TCP权限拦截器
 * 用于统一处理TCP请求的权限验证
 */
@Component
public class TcpPermissionInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TcpPermissionInterceptor.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private RedisCache redisCache;

    /**
     * 检查用户是否有权限访问指定路径
     * 
     * @param path 请求路径
     * @param body 请求体
     * @return 权限检查结果
     */
    public TcpResponse checkPermission(String path, String body) {
        try {
            // 解析请求体获取token
            String token = extractTokenFromBody(body);
            if (StringUtils.isEmpty(token)) {
                return TcpResponse.error("Token不能为空");
            }

            // 获取登录用户信息
            LoginUser loginUser = getLoginUserFromToken(token);
            if (loginUser == null) {
                return TcpResponse.error("用户未登录或登录已过期");
            }

            // 检查路径权限
            String requiredPermission = getRequiredPermission(path);
            if (StringUtils.isNotEmpty(requiredPermission)) {
                if (!hasPermission(loginUser, requiredPermission)) {
                    log.warn("[TcpPermission] 用户 [{}] 没有权限访问 [{}]，需要权限: {}", 
                            loginUser.getUsername(), path, requiredPermission);
                    return TcpResponse.error("权限不足，需要权限: " + requiredPermission);
                }
            }

            return null; // 权限验证通过，返回null表示继续处理
        } catch (Exception e) {
            log.error("[TcpPermission] 权限检查失败: {}", e.getMessage());
            return TcpResponse.error("权限检查失败: " + e.getMessage());
        }
    }

    /**
     * 从请求体中提取token
     * 
     * @param body 请求体
     * @return token
     */
    private String extractTokenFromBody(String body) {
        try {
            if (StringUtils.isNotEmpty(body)) {
                Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
                return (String) params.get("token");
            }
        } catch (JsonProcessingException e) {
            log.warn("解析请求体失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从token获取登录用户信息
     * 
     * @param token 令牌
     * @return 登录用户信息
     */
    private LoginUser getLoginUserFromToken(String token) {
        try {
            // 使用正确的缓存key格式
            String userKey = "login_tokens:" + token;
            LoginUser loginUser = redisCache.getCacheObject(userKey);
            
            if (loginUser == null) {
                log.warn("Token对应的用户信息不存在: {}", token);
                log.debug("尝试获取的缓存key: {}", userKey);
                return null;
            }
            
            // 验证token是否过期
            if (loginUser.getExpireTime() < System.currentTimeMillis()) {
                log.warn("Token已过期: {}", token);
                return null;
            }
            
            return loginUser;
        } catch (Exception e) {
            log.error("从token获取用户信息失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据路径获取所需权限
     * 
     * @param path 请求路径
     * @return 所需权限
     */
    private String getRequiredPermission(String path) {
        if (StringUtils.isEmpty(path)) {
            return null;
        }

        // 用户管理权限
        if (path.startsWith("/system/user/")) {
            if (path.endsWith("/list")) {
                return "system:user:list";
            } else if (path.endsWith("/detail")) {
                return "system:user:query";
            } else if (path.endsWith("/add")) {
                return "system:user:add";
            } else if (path.endsWith("/edit")) {
                return "system:user:edit";
            } else if (path.endsWith("/remove")) {
                return "system:user:remove";
            } else if (path.endsWith("/resetPwd")) {
                return "system:user:resetPwd";
            } else if (path.endsWith("/changeStatus")) {
                return "system:user:edit";
            }
        }
        
        // 角色管理权限
        else if (path.startsWith("/system/role/")) {
            if (path.endsWith("/list")) {
                return "system:role:list";
            } else if (path.endsWith("/detail")) {
                return "system:role:query";
            } else if (path.endsWith("/add")) {
                return "system:role:add";
            } else if (path.endsWith("/edit")) {
                return "system:role:edit";
            } else if (path.endsWith("/remove")) {
                return "system:role:remove";
            } else if (path.endsWith("/changeStatus")) {
                return "system:role:edit";
            } else if (path.endsWith("/dataScope")) {
                return "system:role:edit";
            }
        }
        
        // 菜单管理权限
        else if (path.startsWith("/system/menu/")) {
            if (path.endsWith("/list")) {
                return "system:menu:list";
            } else if (path.endsWith("/detail")) {
                return "system:menu:query";
            } else if (path.endsWith("/add")) {
                return "system:menu:add";
            } else if (path.endsWith("/edit")) {
                return "system:menu:edit";
            } else if (path.endsWith("/remove")) {
                return "system:menu:remove";
            } else if (path.endsWith("/treeselect")) {
                return "system:menu:list";
            } else if (path.endsWith("/roleMenuTreeselect")) {
                return "system:menu:list";
            }
        }

        // 认证相关接口不需要权限验证
        else if (path.startsWith("/auth/")) {
            return null;
        }

        return null;
    }

    /**
     * 检查用户是否拥有指定权限
     * 
     * @param loginUser 登录用户信息
     * @param permission 权限标识
     * @return 是否拥有权限
     */
    private boolean hasPermission(LoginUser loginUser, String permission) {
        if (StringUtils.isEmpty(permission)) {
            return false;
        }
        
        Set<String> permissions = loginUser.getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        
        // 管理员拥有所有权限
        if (permissions.contains("*:*:*")) {
            return true;
        }
        
        // 检查具体权限
        return permissions.contains(permission);
    }
} 