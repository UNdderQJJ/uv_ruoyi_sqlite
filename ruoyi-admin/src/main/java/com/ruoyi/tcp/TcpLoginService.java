package com.ruoyi.tcp;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.framework.web.service.SysPasswordService;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * TCP专用登录服务
 * 避免使用HTTP相关的工具类
 */
@Component
public class TcpLoginService {

    private static final Logger log = LoggerFactory.getLogger(TcpLoginService.class);

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysPasswordService passwordService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    // 令牌有效期（默认30分钟）
    @Value("${token.expireTime:30}")
    private int expireTime;

    /**
     * TCP环境下的用户登录
     * 
     * @param username 用户名
     * @param password 密码
     * @return 登录令牌（UUID）
     */
    public String login(String username, String password) {
        // 验证用户名密码
        SysUser user = validateUser(username, password);
        
        // 创建登录用户对象
        LoginUser loginUser = createLoginUser(user);
        
        // 生成UUID作为token
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        
        // 存储到缓存
        tokenService.refreshToken(loginUser);
        
        log.info("用户 [{}] TCP登录成功，Token: {}", username, token);
        
        return loginUser.getToken();
    }

    /**
     * 验证用户
     * 
     * @param username 用户名
     * @param password 密码
     * @return 用户信息
     */
    private SysUser validateUser(String username, String password) {
        // 查询用户信息
        SysUser user = userService.selectUserByUserName(username);
        if (user == null) {
            log.info("登录用户：{} 不存在.", username);
            throw new ServiceException("用户不存在");
        }

        // 检查用户状态
        if ("1".equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", username);
            throw new ServiceException("用户已被停用");
        }

        if ("2".equals(user.getDelFlag())) {
            log.info("登录用户：{} 已被删除.", username);
            throw new ServiceException("用户已被删除");
        }

        // 验证密码
        if (!SecurityUtils.matchesPassword(password, user.getPassword())) {
            log.info("登录用户：{} 密码错误.", username);
            throw new ServiceException("密码错误");
        }

        return user;
    }

    /**
     * 创建登录用户对象
     * 
     * @param user 用户信息
     * @return 登录用户对象
     */
    private LoginUser createLoginUser(SysUser user) {
        // 获取用户权限
        var permissions = permissionService.getMenuPermission(user);
        
        // 创建登录用户对象
        LoginUser loginUser = new LoginUser(user.getUserId(), user.getDeptId(), user, permissions);
        
        // 设置登录信息（TCP环境简化处理）
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * 60 * 1000); // 30分钟过期
        loginUser.setIpaddr("127.0.0.1"); // TCP环境默认IP
        loginUser.setLoginLocation("TCP客户端");
        loginUser.setBrowser("TCP客户端");
        loginUser.setOs("TCP客户端");
        
        return loginUser;
    }
} 