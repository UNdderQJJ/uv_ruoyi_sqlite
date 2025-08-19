package com.ruoyi.tcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysMenuService;
import io.netty.util.internal.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 菜单管理TCP处理器
 * 专门处理菜单相关的TCP请求
 */
@Component
public class MenuManagementHandler {

    private static final Logger log = LoggerFactory.getLogger(MenuManagementHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ISysMenuService menuService;

    /**
     * 处理菜单管理相关的TCP请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return TCP响应
     */
    public TcpResponse handleMenuRequest(String path, String body) {
        try {
            switch (path) {
                case "/system/menu/list":
                    return listMenus(body);
                case "/system/menu/detail":
                    return getMenuDetail(body);
                case "/system/menu/add":
                    return addMenu(body);
                case "/system/menu/edit":
                    return updateMenu(body);
                case "/system/menu/remove":
                    return removeMenu(body);
                case "/system/menu/treeselect":
                    return getMenuTreeSelect(body);
                case "/system/menu/roleMenuTreeselect":
                    return getRoleMenuTreeSelect(body);
                default:
                    log.warn("[MenuManagement] 未知的菜单管理路径: {}", path);
                    return TcpResponse.error("未知的菜单管理操作: " + path);
            }
        } catch (Exception e) {
            log.error("[MenuManagement] 处理菜单管理请求时发生异常: {}", path, e);
            return TcpResponse.error("菜单管理操作失败: " + e.getMessage());
        }
    }

    /**
     * 查询菜单列表
     */
    private TcpResponse listMenus(String body) throws JsonProcessingException {
        SysMenu menuQuery = new SysMenu();
        if (StringUtils.isNotEmpty(body)) {
            menuQuery = objectMapper.readValue(body, SysMenu.class);
        }
        
        // 获取当前用户ID，这里使用默认值1（管理员）
        Long userId = 1L;
        List<SysMenu> menus = menuService.selectMenuList(menuQuery, userId);
        return TcpResponse.success(menus);
    }

    /**
     * 获取菜单详情
     */
    private TcpResponse getMenuDetail(String body) throws JsonProcessingException {
        SysMenu menuQuery = objectMapper.readValue(body, SysMenu.class);
        SysMenu menu = menuService.selectMenuById(menuQuery.getMenuId());
        if (menu == null) {
            return TcpResponse.error("菜单不存在");
        }
        return TcpResponse.success(menu);
    }

    /**
     * 新增菜单
     */
    private TcpResponse addMenu(String body) throws JsonProcessingException {
        SysMenu menu = objectMapper.readValue(body, SysMenu.class);
        
        // 校验菜单名称唯一性
        if (!menuService.checkMenuNameUnique(menu)) {
            return TcpResponse.error("新增菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        
        // 校验外链地址
        if ("1".equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath())) {
            return TcpResponse.error("新增菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }

        menu.setCreateBy("tcp_admin");
        int rows = menuService.insertMenu(menu);
        return rows > 0 ? TcpResponse.success("新增成功") : TcpResponse.error("新增失败");
    }

    /**
     * 修改菜单
     */
    private TcpResponse updateMenu(String body) throws JsonProcessingException {
        SysMenu menu = objectMapper.readValue(body, SysMenu.class);
        
        // 校验菜单名称唯一性
        if (!menuService.checkMenuNameUnique(menu)) {
            return TcpResponse.error("修改菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        
        // 校验外链地址
        if ("0".equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath())) {
            return TcpResponse.error("修改菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        
        // 校验上级菜单不能选择自己
        if (menu.getMenuId().equals(menu.getParentId())) {
            return TcpResponse.error("修改菜单'" + menu.getMenuName() + "'失败，上级菜单不能选择自己");
        }

        menu.setUpdateBy("tcp_admin");
        int rows = menuService.updateMenu(menu);
        return rows > 0 ? TcpResponse.success("修改成功") : TcpResponse.error("修改失败");
    }

    /**
     * 删除菜单
     */
    private TcpResponse removeMenu(String body) throws JsonProcessingException {
        SysMenu menu = objectMapper.readValue(body, SysMenu.class);
        Long menuId = menu.getMenuId();
        
        // 检查是否存在子菜单
        if (menuService.hasChildByMenuId(menuId)) {
            return TcpResponse.error("存在子菜单,不允许删除");
        }
        
        // 检查菜单是否已分配
        if (menuService.checkMenuExistRole(menuId)) {
            return TcpResponse.error("菜单已分配,不允许删除");
        }
        
        int rows = menuService.deleteMenuById(menuId);
        return rows > 0 ? TcpResponse.success("删除成功") : TcpResponse.error("删除失败");
    }

    /**
     * 获取菜单下拉树列表
     */
    private TcpResponse getMenuTreeSelect(String body) throws JsonProcessingException {
        SysMenu menuQuery = new SysMenu();
        if (StringUtils.isNotEmpty(body)) {
            menuQuery = objectMapper.readValue(body, SysMenu.class);
        }
        
        // 获取当前用户ID，这里使用默认值1（管理员）
        Long userId = 1L;
        List<SysMenu> menus = menuService.selectMenuList(menuQuery, userId);
        return TcpResponse.success(menuService.buildMenuTreeSelect(menus));
    }

    /**
     * 加载对应角色菜单列表树
     */
    private TcpResponse getRoleMenuTreeSelect(String body) throws JsonProcessingException {
        SysRole role = objectMapper.readValue(body, SysRole.class);

        Long roleId = role.getRoleId();
        
        // 获取当前用户ID，这里使用默认值1（管理员）
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        if(Objects.isNull(params.get("userId").toString())){
            return TcpResponse.error("userId不能为空");
        }
        Long userId = Long.valueOf(params.get("userId").toString());


        List<SysMenu> menus = menuService.selectMenuList(userId);
        
        // 构建返回结果
        var result = new java.util.HashMap<String, Object>();
        result.put("checkedKeys", menuService.selectMenuListByRoleId(roleId));
        result.put("menus", menuService.buildMenuTreeSelect(menus));
        
        return TcpResponse.success(result);
    }
} 