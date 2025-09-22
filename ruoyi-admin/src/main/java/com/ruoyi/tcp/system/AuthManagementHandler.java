package com.ruoyi.tcp.system;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysMenuService;
import com.ruoyi.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限控制TCP处理器
 * 专门处理登录认证和权限相关的TCP请求
 */
@Component
public class AuthManagementHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthManagementHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private TcpLoginService tcpLoginService;

    @Autowired
    private RedisCache redisCache;

    /**
     * 处理权限控制相关的TCP请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return TCP响应
     */
    public TcpResponse handleAuthRequest(String path, String body) {
        try {
            switch (path) {
                case "/auth/login":
                    return login(body);
                case "/auth/getInfo":
                    return getUserInfo(body);
                case "/auth/getRouters":
                    return getRouters(body);
                case "/auth/logout":
                    return logout(body);
                case "/auth/checkPermission":
                    return checkPermission(body);
                case "/auth/validateToken":
                    return validateToken(body);
                default:
                    log.warn("[AuthManagement] 未知的权限控制路径: {}", path);
                    return TcpResponse.error("未知的权限控制操作: " + path);
            }
        } catch (Exception e) {
            log.error("[AuthManagement] 处理权限控制请求时发生异常: {}", path, e);
            return TcpResponse.error("权限控制操作失败: " + e.getMessage());
        }
    }

    /**
     * 用户登录（TCP环境专用）
     */
    private TcpResponse login(String body) throws JsonProcessingException {
        LoginBody loginBody = objectMapper.readValue(body, LoginBody.class);
        
        try {
            // 使用TCP专用登录服务
            String token = tcpLoginService.login(loginBody.getUsername(), loginBody.getPassword());
             // 获取登录用户信息
            LoginUser loginUser = getLoginUserFromToken(token);
            // 构建返回结果
            var result = new java.util.HashMap<String, Object>();
            result.put("token", token);
            if (loginUser != null) {
                result.put("userId", loginUser.getUserId());
                result.put("username", loginUser.getUsername());
            }
            result.put("msg", "登录成功");
            
            return TcpResponse.success(result);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            return TcpResponse.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户信息
     */
    private TcpResponse getUserInfo(String body) throws JsonProcessingException {
        // 从body中获取token
        String token = null;
        if (StringUtils.isNotEmpty(body)) {
            Map<String, String> params = objectMapper.readValue(body, new TypeReference<>() {});
            token = params.get("token");
        }
        
        // 如果没有提供token，返回错误
        if (StringUtils.isEmpty(token)) {
            return TcpResponse.error("Token不能为空");
        }
        
        // 获取登录用户信息
        try {
            LoginUser loginUser = getLoginUserFromToken(token);
            if (loginUser == null) {
//                return TcpResponse.error("用户未登录或登录已过期");
                return null;
            }
            
            SysUser user = loginUser.getUser();
            // 角色集合
            Set<String> roles = permissionService.getRolePermission(user);
            // 权限集合
            Set<String> permissions = permissionService.getMenuPermission(user);
            if (!loginUser.getPermissions().equals(permissions))
            {
                loginUser.setPermissions(permissions);
                tokenService.refreshToken(loginUser);
            }
            
            var result = new java.util.HashMap<String, Object>();
            result.put("user", user);
            result.put("roles", roles);
            result.put("permissions", permissions);
            
            return TcpResponse.success(result);
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            return TcpResponse.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取路由信息
     */
    private TcpResponse getRouters(String body) throws JsonProcessingException {
        // 从body中获取token
        String token = null;
        if (StringUtils.isNotEmpty(body)) {
            Map<String, String> params = objectMapper.readValue(body, new TypeReference<>() {});
            token = params.get("token");
        }
        
        if (StringUtils.isEmpty(token)) {
            return TcpResponse.error("Token不能为空");
        }
        
        try {
            // 获取登录用户信息
            LoginUser loginUser = getLoginUserFromToken(token);
            if (loginUser == null) {
                return TcpResponse.error("用户未登录或登录已过期");
            }
            
            Long userId = loginUser.getUserId();
            List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
            
            return TcpResponse.success(menuService.buildMenus(menus));
        } catch (Exception e) {
            log.error("获取路由信息失败: {}", e.getMessage());
            return TcpResponse.error("获取路由信息失败: " + e.getMessage());
        }
    }

    /**
     * 用户登出
     */
    private TcpResponse logout(String body) throws JsonProcessingException {
        String token = null;
        if (StringUtils.isNotEmpty(body)) {
            Map<String, String> params = objectMapper.readValue(body, new TypeReference<>() {});
            token = params.get("token");
        }
        
        if (StringUtils.isNotEmpty(token)) {
            // 删除用户身份记录
            try {
                tokenService.delLoginUser(token);
            } catch (Exception e) {
                log.warn("删除登录用户信息失败: {}", e.getMessage());
            }
        }
        
        return TcpResponse.success("登出成功");
    }

    /**
     * 检查用户权限
     */
    private TcpResponse checkPermission(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        String token = (String) params.get("token");
        String permission = (String) params.get("permission");
        
        if (StringUtils.isEmpty(token)) {
            return TcpResponse.error("Token不能为空");
        }
        
        if (StringUtils.isEmpty(permission)) {
            return TcpResponse.error("权限标识不能为空");
        }
        
        try {
            // 获取登录用户信息
            LoginUser loginUser = getLoginUserFromToken(token);
            if (loginUser == null) {
                return TcpResponse.error("用户未登录或登录已过期");
            }
            
            // 检查权限
            boolean hasPermission = hasPermission(loginUser, permission);
            
            var result = new java.util.HashMap<String, Object>();
            result.put("hasPermission", hasPermission);
            result.put("permission", permission);
            result.put("userId", loginUser.getUserId());
            result.put("username", loginUser.getUsername());
            
            return TcpResponse.success(result);
        } catch (Exception e) {
            log.error("检查权限失败: {}", e.getMessage());
            return TcpResponse.error("检查权限失败: " + e.getMessage());
        }
    }

    /**
     * 验证Token有效性
     */
    private TcpResponse validateToken(String body) throws JsonProcessingException {
        String token = null;
        if (StringUtils.isNotEmpty(body)) {
            Map<String, String> params = objectMapper.readValue(body, new TypeReference<>() {});
            token = params.get("token");
        }
        
        if (StringUtils.isEmpty(token)) {
            return TcpResponse.error("Token不能为空");
        }
        
        try {
            // 获取登录用户信息
            LoginUser loginUser = getLoginUserFromToken(token);
            if (loginUser == null) {
                return TcpResponse.error("Token无效或已过期");
            }
            
            var result = new java.util.HashMap<String, Object>();
            result.put("valid", true);
            result.put("userId", loginUser.getUserId());
            result.put("username", loginUser.getUsername());
            result.put("expireTime", loginUser.getExpireTime());
            
            return TcpResponse.success(result);
        } catch (Exception e) {
            log.error("验证Token失败: {}", e.getMessage());
            return TcpResponse.error("验证Token失败: " + e.getMessage());
        }
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
            
//            // 验证token是否过期
//            if (loginUser.getExpireTime() < System.currentTimeMillis()) {
//                log.warn("Token已过期: {}", token);
//                return null;
//            }
            
            return loginUser;
        } catch (Exception e) {
            log.error("从token获取用户信息失败: {}", e.getMessage());
            return null;
        }
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