package com.ruoyi.tcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.TcpRequest;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysPostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理TCP处理器
 * 专门处理用户相关的TCP请求
 */
@Component
public class UserManagementHandler {

    private static final Logger log = LoggerFactory.getLogger(UserManagementHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysPostService postService;

    /**
     * 处理用户管理相关的TCP请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return TCP响应
     */
    public TcpResponse handleUserRequest(String path, String body) {
        try {
            switch (path) {
                case "/system/user/list":
                    return listUsers(body);
                case "/system/user/detail":
                    return getUserDetail(body);
                case "/system/user/add":
                    return addUser(body);
                case "/system/user/edit":
                    return updateUser(body);
                case "/system/user/remove":
                    return removeUsers(body);
                case "/system/user/resetPwd":
                    return resetUserPassword(body);
                case "/system/user/changeStatus":
                    return changeUserStatus(body);
                default:
                    log.warn("[UserManagement] 未知的用户管理路径: {}", path);
                    return TcpResponse.error("未知的用户管理操作: " + path);
            }
        } catch (Exception e) {
            log.error("[UserManagement] 处理用户管理请求时发生异常: {}", path, e);
            return TcpResponse.error("用户管理操作失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户列表 (带分页)
     */
    private TcpResponse listUsers(String body) throws JsonProcessingException {
        // 默认分页参数
        Integer pageNum = 1;
        Integer pageSize = 10;
        String orderBy = null;
        SysUser userQuery = new SysUser();

        if (StringUtils.isNotEmpty(body)) {
            Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});

            // 安全地获取分页参数
            Object pageNumObj = params.get("pageNum");
            if (pageNumObj instanceof Integer) {
                pageNum = (Integer) pageNumObj;
            }

            Object pageSizeObj = params.get("pageSize");
            if (pageSizeObj instanceof Integer) {
                pageSize = (Integer) pageSizeObj;
            }

            // 获取排序字段
            Object orderByColumnObj = params.get("orderByColumn");
            Object isAscObj = params.get("isAsc");
            if (orderByColumnObj != null && isAscObj != null) {
                orderBy = StringUtils.toUnderScoreCase(orderByColumnObj.toString()) + " " + isAscObj.toString();
            }

            userQuery = objectMapper.convertValue(params, SysUser.class);
        }

        // 启动分页
        PageHelper.startPage(pageNum, pageSize, orderBy);

        // 执行查询
        List<SysUser> list = userMapper.selectUserList(userQuery);

        // 封装结果
        TableDataInfo tableDataInfo = new TableDataInfo();
        tableDataInfo.setCode(200);
        tableDataInfo.setMsg("查询成功");
        tableDataInfo.setRows(list);
        tableDataInfo.setTotal(((Page<?>) list).getTotal());

        return TcpResponse.success(tableDataInfo);
    }

    /**
     * 获取用户详情
     */
    private TcpResponse getUserDetail(String body) throws JsonProcessingException {
        SysUser user = objectMapper.readValue(body, SysUser.class);
        Long userId = user.getUserId();

         // 构建详细响应数据
         Map<String, Object> data;
        
        if (StringUtils.isNotNull(userId)) {
//            // 检查用户数据权限
//            userService.checkUserDataScope(userId);

            // 获取用户基本信息
            SysUser sysUser = userService.selectUserById(userId);
            if (sysUser == null) {
                return TcpResponse.error("用户不存在");
            }

            // 构建详细响应数据
            data = Map.of(
                    "user", sysUser,
                    "postIds", postService.selectPostListByUserId(userId),
                    "roleIds", sysUser.getRoles().stream().map(SysRole::getRoleId).collect(Collectors.toList()),
                    "roles", SysUser.isAdmin(userId) ? roleService.selectRoleAll() :
                            roleService.selectRoleAll().stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()),
                    "posts", postService.selectPostAll()
            );
        }else {
             data = Map.of(
                     "roles", SysUser.isAdmin(userId) ? roleService.selectRoleAll() :
                        roleService.selectRoleAll().stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()),
                "posts", postService.selectPostAll()
             );
        }
            return TcpResponse.success(data);
    }

    /**
     * 新增用户
     */
    private TcpResponse addUser(String body) throws JsonProcessingException {
        SysUser user = objectMapper.readValue(body, SysUser.class);
        
        // 校验用户名唯一性
        if (!userService.checkUserNameUnique(user)) {
            return TcpResponse.error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        
        // 校验手机号唯一性
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user)) {
            return TcpResponse.error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        
        // 校验邮箱唯一性
        if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user)) {
            return TcpResponse.error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }

        user.setCreateBy("tcp_admin");
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        int rows = userService.insertUser(user);
        return rows > 0 ? TcpResponse.success("新增成功") : TcpResponse.error("新增失败");
    }

    /**
     * 修改用户
     */
    private TcpResponse updateUser(String body) throws JsonProcessingException {
        SysUser user = objectMapper.readValue(body, SysUser.class);
        
        // 检查是否允许操作
        userService.checkUserAllowed(user);
        
        // 校验用户名唯一性
        if (!userService.checkUserNameUnique(user)) {
            return TcpResponse.error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        
        // 校验手机号唯一性
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user)) {
            return TcpResponse.error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        
        // 校验邮箱唯一性
        if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user)) {
            return TcpResponse.error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }

        user.setUpdateBy("tcp_admin");
         user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        int rows = userService.updateUser(user);
        return rows > 0 ? TcpResponse.success("修改成功") : TcpResponse.error("修改失败");
    }

    /**
     * 删除用户
     */
    private TcpResponse removeUsers(String body) throws JsonProcessingException {
        SysUser user = objectMapper.readValue(body, SysUser.class);
        Long userId =user.getUserId();
        int rows = userService.deleteUserById(userId);
        return rows > 0 ? TcpResponse.success("删除成功") : TcpResponse.error("删除失败");
    }

    /**
     * 重置用户密码
     */
    private TcpResponse resetUserPassword(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long userId = Long.valueOf(params.get("userId").toString());
        String newPassword = params.get("password").toString();
        
        // 先获取用户信息
        SysUser user = userService.selectUserById(userId);
        if (user == null) {
            return TcpResponse.error("用户不存在");
        }
        
        // 使用正确的方法签名
        int rows = userService.resetUserPwd(user.getUserName(), SecurityUtils.encryptPassword(newPassword));
        return rows > 0 ? TcpResponse.success("密码重置成功") : TcpResponse.error("密码重置失败");
    }

    /**
     * 修改用户状态
     */
    private TcpResponse changeUserStatus(String body) throws JsonProcessingException {
        SysUser user = objectMapper.readValue(body, SysUser.class);
        userService.checkUserAllowed(user);
        user.setUpdateBy("tcp_admin");
        
        int rows = userService.updateUserStatus(user);
        return rows > 0 ? TcpResponse.success("状态修改成功") : TcpResponse.error("状态修改失败");
    }
} 