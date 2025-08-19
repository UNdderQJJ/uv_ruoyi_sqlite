package com.ruoyi.tcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 角色管理TCP处理器
 * 专门处理角色相关的TCP请求
 */
@Component
public class RoleManagementHandler {

    private static final Logger log = LoggerFactory.getLogger(RoleManagementHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ISysRoleService roleService;

    /**
     * 处理角色管理相关的TCP请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return TCP响应
     */
    public TcpResponse handleRoleRequest(String path, String body) {
        try {
            switch (path) {
                case "/system/role/list":
                    return listRoles(body);
                case "/system/role/detail":
                    return getRoleDetail(body);
                case "/system/role/add":
                    return addRole(body);
                case "/system/role/edit":
                    return updateRole(body);
                case "/system/role/remove":
                    return removeRoles(body);
                case "/system/role/changeStatus":
                    return changeRoleStatus(body);
                case "/system/role/dataScope":
                    return updateDataScope(body);
                default:
                    log.warn("[RoleManagement] 未知的角色管理路径: {}", path);
                    return TcpResponse.error("未知的角色管理操作: " + path);
            }
        } catch (Exception e) {
            log.error("[RoleManagement] 处理角色管理请求时发生异常: {}", path, e);
            return TcpResponse.error("角色管理操作失败: " + e.getMessage());
        }
    }

    /**
     * 查询角色列表 (带分页)
     */
    private TcpResponse listRoles(String body) throws JsonProcessingException {
        // 默认分页参数
        Integer pageNum = 1;
        Integer pageSize = 10;
        String orderBy = null;
        SysRole roleQuery = new SysRole();

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

            roleQuery = objectMapper.convertValue(params, SysRole.class);
        }

        // 启动分页
        PageHelper.startPage(pageNum, pageSize, orderBy);

        // 执行查询
        List<SysRole> list = roleService.selectRoleList(roleQuery);

        // 封装结果
        TableDataInfo tableDataInfo = new TableDataInfo();
        tableDataInfo.setCode(200);
        tableDataInfo.setMsg("查询成功");
        tableDataInfo.setRows(list);
        tableDataInfo.setTotal(((Page<?>) list).getTotal());

        return TcpResponse.success(tableDataInfo);
    }

    /**
     * 获取角色详情
     */
    private TcpResponse getRoleDetail(String body) throws JsonProcessingException {
        SysRole roleId = objectMapper.readValue(body, SysRole.class);
        SysRole role = roleService.selectRoleById(roleId.getRoleId());
        if (role == null) {
            return TcpResponse.error("角色不存在");
        }
        return TcpResponse.success(role);
    }

    /**
     * 新增角色
     */
    private TcpResponse addRole(String body) throws JsonProcessingException {
        SysRole role = objectMapper.readValue(body, SysRole.class);
        
        // 校验角色名称唯一性
        if (!roleService.checkRoleNameUnique(role)) {
            return TcpResponse.error("新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        
        // 校验角色权限唯一性
        if (!roleService.checkRoleKeyUnique(role)) {
            return TcpResponse.error("新增角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }

        role.setCreateBy("tcp_admin");
        int rows = roleService.insertRole(role);
        return rows > 0 ? TcpResponse.success("新增成功") : TcpResponse.error("新增失败");
    }

    /**
     * 修改角色
     */
    private TcpResponse updateRole(String body) throws JsonProcessingException {
        SysRole role = objectMapper.readValue(body, SysRole.class);
        
        // 检查是否允许操作
        roleService.checkRoleAllowed(role);
        
        // 校验角色名称唯一性
        if (!roleService.checkRoleNameUnique(role)) {
            return TcpResponse.error("修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        
        // 校验角色权限唯一性
        if (!roleService.checkRoleKeyUnique(role)) {
            return TcpResponse.error("修改角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }

        role.setUpdateBy("tcp_admin");
        int rows = roleService.updateRole(role);
        return rows > 0 ? TcpResponse.success("修改成功") : TcpResponse.error("修改失败");
    }

    /**
     * 删除角色
     */
    private TcpResponse removeRoles(String body) throws JsonProcessingException {
        SysRole role = objectMapper.readValue(body,SysRole.class);
        Long roleId = role.getRoleId();
        int rows = roleService.deleteRoleById(roleId);
        return rows > 0 ? TcpResponse.success("删除成功") : TcpResponse.error("删除失败");
    }

    /**
     * 修改角色状态
     */
    private TcpResponse changeRoleStatus(String body) throws JsonProcessingException {
        SysRole role = objectMapper.readValue(body, SysRole.class);
        roleService.checkRoleAllowed(role);
        role.setUpdateBy("tcp_admin");
        
        int rows = roleService.updateRoleStatus(role);
        return rows > 0 ? TcpResponse.success("状态修改成功") : TcpResponse.error("状态修改失败");
    }

    /**
     * 修改数据权限
     */
    private TcpResponse updateDataScope(String body) throws JsonProcessingException {
        SysRole role = objectMapper.readValue(body, SysRole.class);
        roleService.checkRoleAllowed(role);
        
        int rows = roleService.authDataScope(role);
        return rows > 0 ? TcpResponse.success("数据权限修改成功") : TcpResponse.error("数据权限修改失败");
    }
} 