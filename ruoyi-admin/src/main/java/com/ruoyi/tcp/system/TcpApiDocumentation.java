package com.ruoyi.tcp.system;

/**
 * TCP API 接口文档
 * 
 * 本系统通过Netty TCP服务器提供用户管理、角色管理、菜单管理和权限控制功能
 * 
 * 请求格式：
 * {
 *   "path": "/api/path",
 *   "body": "请求体内容（JSON字符串）"
 * }
 * 
 * 响应格式：
 * {
 *   "code": 200,
 *   "msg": "操作成功",
 *   "data": {}
 * }
 * 
 * 权限说明：
 * 1. 除了登录接口外，所有接口都需要在请求体中包含token字段
 * 2. 系统会自动验证用户权限，权限不足会返回错误信息
 * 3. 权限格式：模块:功能:操作，如 system:user:list
 * 4. 管理员拥有所有权限（*:*:*）
 * 
 * @author ruoyi
 */
public class TcpApiDocumentation {

    /**
     * ==================== 用户管理接口 ====================
     * 所需权限：system:user:*
     */
    
    /**
     * 查询用户列表
     * 路径: /system/user/list
     * 方法: GET
     * 权限: system:user:list
     * 请求体: {
     *   "token": "登录令牌",
     *   "pageNum": 1,
     *   "pageSize": 10,
     *   "orderByColumn": "createTime",
     *   "isAsc": "desc",
     *   "userName": "admin",
     *   "phonenumber": "13800138000",
     *   "status": "0"
     * }
     */
    
    /**
     * 获取用户详情
     * 路径: /system/user/detail
     * 方法: GET
     * 权限: system:user:query
     * 请求体: {
     *   "token": "登录令牌",
     *   "userId": 1
     * }
     */
    
    /**
     * 新增用户
     * 路径: /system/user/add
     * 方法: POST
     * 权限: system:user:add
     * 请求体: {
     *   "token": "登录令牌",
     *   "userName": "testuser",
     *   "nickName": "测试用户",
     *   "password": "123456",
     *   "email": "test@example.com",
     *   "phonenumber": "13800138000",
     *   "deptId": 100,
     *   "roleIds": [1, 2],
     *   "postIds": [1, 2],
     *   "status": "0"
     * }
     */
    
    /**
     * 修改用户
     * 路径: /system/user/edit
     * 方法: PUT
     * 权限: system:user:edit
     * 请求体: {
     *   "token": "登录令牌",
     *   "userId": 1,
     *   "userName": "testuser",
     *   "nickName": "测试用户",
     *   "email": "test@example.com",
     *   "phonenumber": "13800138000",
     *   "deptId": 100,
     *   "roleIds": [1, 2],
     *   "postIds": [1, 2],
     *   "status": "0"
     * }
     */
    
    /**
     * 删除用户
     * 路径: /system/user/remove
     * 方法: DELETE
     * 权限: system:user:remove
     * 请求体: {
     *   "token": "登录令牌",
     *   "userIds": [1, 2, 3]
     * }
     */
    
    /**
     * 重置用户密码
     * 路径: /system/user/resetPwd
     * 方法: PUT
     * 权限: system:user:resetPwd
     * 请求体: {
     *   "token": "登录令牌",
     *   "userId": 1,
     *   "password": "newpassword"
     * }
     */
    
    /**
     * 修改用户状态
     * 路径: /system/user/changeStatus
     * 方法: PUT
     * 权限: system:user:edit
     * 请求体: {
     *   "token": "登录令牌",
     *   "userId": 1,
     *   "status": "1"
     * }
     */

    /**
     * ==================== 角色管理接口 ====================
     * 所需权限：system:role:*
     */
    
    /**
     * 查询角色列表
     * 路径: /system/role/list
     * 方法: GET
     * 权限: system:role:list
     * 请求体: {
     *   "token": "登录令牌",
     *   "pageNum": 1,
     *   "pageSize": 10,
     *   "orderByColumn": "createTime",
     *   "isAsc": "desc",
     *   "roleName": "管理员",
     *   "roleKey": "admin",
     *   "status": "0"
     * }
     */
    
    /**
     * 获取角色详情
     * 路径: /system/role/detail
     * 方法: GET
     * 权限: system:role:query
     * 请求体: {
     *   "token": "登录令牌",
     *   "roleId": 1
     * }
     */
    
    /**
     * 新增角色
     * 路径: /system/role/add
     * 方法: POST
     * 权限: system:role:add
     * 请求体: {
     *   "token": "登录令牌",
     *   "roleName": "测试角色",
     *   "roleKey": "test",
     *   "roleSort": 1,
     *   "dataScope": "1",
     *   "menuIds": [1, 2, 3],
     *   "deptIds": [100, 101],
     *   "status": "0",
     *   "remark": "测试角色"
     * }
     */
    
    /**
     * 修改角色
     * 路径: /system/role/edit
     * 方法: PUT
     * 权限: system:role:edit
     * 请求体: {
     *   "token": "登录令牌",
     *   "roleId": 1,
     *   "roleName": "测试角色",
     *   "roleKey": "test",
     *   "roleSort": 1,
     *   "dataScope": "1",
     *   "menuIds": [1, 2, 3],
     *   "deptIds": [100, 101],
     *   "status": "0",
     *   "remark": "测试角色"
     * }
     */
    
    /**
     * 删除角色
     * 路径: /system/role/remove
     * 方法: DELETE
     * 权限: system:role:remove
     * 请求体: {
     *   "token": "登录令牌",
     *   "roleIds": [1, 2, 3]
     * }
     */
    
    /**
     * 修改角色状态
     * 路径: /system/role/changeStatus
     * 方法: PUT
     * 权限: system:role:edit
     * 请求体: {
     *   "token": "登录令牌",
     *   "roleId": 1,
     *   "status": "1"
     * }
     */
    
    /**
     * 修改数据权限
     * 路径: /system/role/dataScope
     * 方法: PUT
     * 权限: system:role:edit
     * 请求体: {
     *   "token": "登录令牌",
     *   "roleId": 1,
     *   "dataScope": "2",
     *   "deptIds": [100, 101]
     * }
     */

    /**
     * ==================== 菜单管理接口 ====================
     * 所需权限：system:menu:*
     */
    
    /**
     * 查询菜单列表
     * 路径: /system/menu/list
     * 方法: GET
     * 权限: system:menu:list
     * 请求体: {
     *   "token": "登录令牌",
     *   "menuName": "用户管理",
     *   "status": "0"
     * }
     */
    
    /**
     * 获取菜单详情
     * 路径: /system/menu/detail
     * 方法: GET
     * 权限: system:menu:query
     * 请求体: {
     *   "token": "登录令牌",
     *   "menuId": 1
     * }
     */
    
    /**
     * 新增菜单
     * 路径: /system/menu/add
     * 方法: POST
     * 权限: system:menu:add
     * 请求体: {
     *   "token": "登录令牌",
     *   "menuName": "测试菜单",
     *   "parentId": 0,
     *   "orderNum": 1,
     *   "path": "/test",
     *   "component": "system/test/index",
     *   "isFrame": "1",
     *   "isCache": "0",
     *   "menuType": "C",
     *   "visible": "0",
     *   "status": "0",
     *   "perms": "system:test:list",
     *   "icon": "test"
     * }
     */
    
    /**
     * 修改菜单
     * 路径: /system/menu/edit
     * 方法: PUT
     * 权限: system:menu:edit
     * 请求体: {
     *   "token": "登录令牌",
     *   "menuId": 1,
     *   "menuName": "测试菜单",
     *   "parentId": 0,
     *   "orderNum": 1,
     *   "path": "/test",
     *   "component": "system/test/index",
     *   "isFrame": "1",
     *   "isCache": "0",
     *   "menuType": "C",
     *   "visible": "0",
     *   "status": "0",
     *   "perms": "system:test:list",
     *   "icon": "test"
     * }
     */
    
    /**
     * 删除菜单
     * 路径: /system/menu/remove
     * 方法: DELETE
     * 权限: system:menu:remove
     * 请求体: {
     *   "token": "登录令牌",
     *   "menuId": 1
     * }
     */
    
    /**
     * 获取菜单下拉树列表
     * 路径: /system/menu/treeselect
     * 方法: GET
     * 权限: system:menu:list
     * 请求体: {
     *   "token": "登录令牌",
     *   "menuName": "用户管理"
     * }
     */
    
    /**
     * 加载对应角色菜单列表树
     * 路径: /system/menu/roleMenuTreeselect
     * 方法: GET
     * 权限: system:menu:list
     * 请求体: {
     *   "token": "登录令牌",
     *   "roleId": 1
     * }
     */

    /**
     * ==================== 权限控制接口 ====================
     * 注意：权限控制接口不需要token验证
     */
    
    /**
     * 用户登录
     * 路径: /auth/login
     * 方法: POST
     * 权限: 无需权限
     * 请求体: {
     *   "username": "admin",
     *   "password": "123456",
     *   "code": "验证码",
     *   "uuid": "验证码标识"
     * }
     */
    
    /**
     * 获取用户信息
     * 路径: /auth/getInfo
     * 方法: GET
     * 权限: 无需权限
     * 请求体: {
     *   "token": "登录令牌"
     * }
     */
    
    /**
     * 获取路由信息
     * 路径: /auth/getRouters
     * 方法: GET
     * 权限: 无需权限
     * 请求体: {
     *   "token": "登录令牌"
     * }
     */
    
    /**
     * 用户登出
     * 路径: /auth/logout
     * 方法: POST
     * 权限: 无需权限
     * 请求体: {
     *   "token": "登录令牌"
     * }
     */
    
    /**
     * 检查用户权限
     * 路径: /auth/checkPermission
     * 方法: POST
     * 权限: 无需权限
     * 请求体: {
     *   "token": "登录令牌",
     *   "permission": "system:user:list"
     * }
     */

    /**
     * 获取用户所有权限
     * 路径: /auth/getUserPermissions
     * 方法: GET
     * 权限: 无需权限
     * 请求体: {
     *   "token": "登录令牌"
     * }
     */

    /**
     * 验证Token有效性
     * 路径: /auth/validateToken
     * 方法: POST
     * 权限: 无需权限
     * 请求体: {
     *   "token": "登录令牌"
     * }
     */

    /**
     * ==================== 权限标识说明 ====================
     * 
     * 权限格式：模块:功能:操作
     * 
     * 用户管理权限：
     * - system:user:list     - 用户列表查询
     * - system:user:query    - 用户详情查询
     * - system:user:add      - 用户新增
     * - system:user:edit     - 用户修改
     * - system:user:remove   - 用户删除
     * - system:user:resetPwd - 用户密码重置
     * 
     * 角色管理权限：
     * - system:role:list     - 角色列表查询
     * - system:role:query    - 角色详情查询
     * - system:role:add      - 角色新增
     * - system:role:edit     - 角色修改
     * - system:role:remove   - 角色删除
     * 
     * 菜单管理权限：
     * - system:menu:list     - 菜单列表查询
     * - system:menu:query    - 菜单详情查询
     * - system:menu:add      - 菜单新增
     * - system:menu:edit     - 菜单修改
     * - system:menu:remove   - 菜单删除
     * 
     * 特殊权限：
     * - *:*:*               - 超级管理员权限（拥有所有权限）
     * - system:*:*          - 系统管理权限
     */

    /**
     * ==================== 状态码说明 ====================
     * 
     * 200: 操作成功
     * 500: 服务器内部错误
     * 400: 请求参数错误
     * 401: 未授权（Token无效或过期）
     * 403: 权限不足
     * 404: 资源不存在
     * 
     * ==================== 注意事项 ====================
     * 
     * 1. 所有请求都需要通过TCP连接发送
     * 2. 请求和响应都是JSON格式
     * 3. 除了登录接口外，所有接口都需要在请求体中包含token字段
     * 4. 分页参数：pageNum从1开始，pageSize默认10
     * 5. 用户状态：0-正常，1-停用
     * 6. 角色状态：0-正常，1-停用
     * 7. 菜单类型：M-目录，C-菜单，F-按钮
     * 8. 数据权限：1-全部数据权限，2-自定数据权限，3-本部门数据权限，4-本部门及以下数据权限，5-仅本人数据权限
     * 9. 权限验证失败会返回403状态码和具体错误信息
     * 10. Token过期需要重新登录获取新的Token
     */
} 