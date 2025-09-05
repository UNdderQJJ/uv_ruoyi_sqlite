package com.ruoyi.tcp.business.Task;

/**
 * 任务中心 TaskInfo TCP API 文档
 *
 * 本文档参考 {@link com.ruoyi.tcp.business.DeviceFileConfig.DeviceFileConfigTcpApiDocumentation} 的风格与结构。
 *
 * 所有请求均为 TCP 文本消息，顶层 JSON 格式：
 * {
 *   "id": "请求ID",
 *   "path": "接口路径",
 *   "body": "请求体JSON字符串"
 * }
 *
 * 响应统一封装：
 * {
 *   "id": "请求ID",
 *   "code": 200,
 *   "message": "响应消息",
 *   "data": {}
 * }
 */
public class TaskInfoTcpApiDocumentation {

    /**
     * 1) 创建任务
     *
     * 请求路径: /business/taskInfo/create
     * 请求体: TaskInfo 对象(无需 id、createTime、updateTime、delFlag)
     * {
     *   "name": "任务名称",
     *   "status": "PENDING",
     *   "poolId": 1,
     *   "poolName": "原料池A",
     *   "plannedQuantity": -1,
     *   "preloadDataCount": 100,
     *   "description": "任务备注"
     * }
     *
     * 可选扩展参数（用于创建任务-设备关联 task_device_link）:
     * {
     *   "deviceIds": [1,2,3],           // 关联的设备ID集合
     *   "assignedQuantity": 1000,       // 分配给每台设备的打印数量（可选）
     *   "deviceFileConfigId": 10,       // 指定文件配置ID（可选）
     *   "poolTemplateId": 5             // 指定数据池模版ID（可选）
     * }
     *
     * 响应数据: 创建后的 TaskInfo
     *
     * 示例：
     * {
     *   "id": "req_task_001",
     *   "path": "/business/taskInfo/create",
     *   "body": "{\"name\":\"任务1\",\"poolId\":1,\"plannedQuantity\":-1,\"deviceIds\":[1,2],\"assignedQuantity\":500}"
     * }
     */

    /**
     * 2) 更新任务（支持增加或减少设备关联）
     *
     * 请求路径: /business/taskInfo/update
     * 请求体: TaskInfo 对象(必须包含 id)
     * {
     *   "id": 1,
     *   "name": "任务名称-更新",
     *   "plannedQuantity": 1000
     * }
     *
     * 可选扩展参数（用于更新任务-设备关联）:
     * {
     *   "deviceIds": [1,2,3,4],         // 新的设备ID集合（会替换现有关联）
     *   "assignedQuantity": 1500,       // 分配给每台设备的打印数量（可选）
     *   "deviceFileConfigId": 15,       // 指定文件配置ID（可选）
     *   "poolTemplateId": 8             // 指定数据池模版ID（可选）
     * }
     * 
     * 注意：如果提供了 deviceIds，会先删除该任务的所有现有设备关联，然后重新创建新的关联
     * 如果 deviceIds 为空数组 []，则只删除现有关联，不创建新关联
     * 如果不提供 deviceIds 字段，则不影响现有设备关联
     *
     * 响应数据: 更新后的 TaskInfo
     *
     * 示例：
     * 1) 只更新任务基本信息
     * {
     *   "id": "req_task_002",
     *   "path": "/business/taskInfo/update",
     *   "body": "{\"id\":1,\"name\":\"任务名称-更新\"}"
     * }
     * 
     * 2) 更新任务并重新分配设备
     * {
     *   "id": "req_task_003",
     *   "path": "/business/taskInfo/update",
     *   "body": "{\"id\":1,\"name\":\"任务名称-更新\",\"deviceIds\":[1,2,3],\"assignedQuantity\":800}"
     * }
     * 
     * 3) 更新任务并移除所有设备关联
     * {
     *   "id": "req_task_004",
     *   "path": "/business/taskInfo/update",
     *   "body": "{\"id\":1,\"name\":\"任务名称-更新\",\"deviceIds\":[]}"
     * }
     */

    /**
     * 3) 删除任务（批量软删）
     *
     * 请求路径: /business/taskInfo/delete
     * 请求体:
     * { "ids": [1,2,3] }
     *
     * 响应数据: 受影响行数
     *
     * 示例：
     * {
     *   "id": "req_task_003",
     *   "path": "/business/taskInfo/delete",
     *   "body": "[1,2,3]"
     * }
     */

    /**
     * 4) 获取任务详情
     *
     * 请求路径: /business/taskInfo/get
     * 请求体: { "id": 1 } 或直接传 long 字符串 "1"
     *
     * 响应数据: TaskInfo
     *
     * 示例：
     * {
     *   "id": "req_task_004",
     *   "path": "/business/taskInfo/get",
     *   "body": "{\"id\":1}"
     * }
     */

    /**
     * 5) 查询任务列表（支持条件）
     *
     * 请求路径: /business/taskInfo/list
     * 请求体(可选条件):
     * {
     *   "name": "关键字",
     *   "status": "RUNNING",
     *   "poolId": 1
     * }
     *
     * 响应数据: [TaskInfo]
     *
     * 示例：
     * {
     *   "id": "req_task_005",
     *   "path": "/business/taskInfo/list",
     *   "body": "{\"status\":\"PENDING\"}"
     * }
     */

    /**
     * 6) 统计任务数量（支持条件）
     *
     * 请求路径: /business/taskInfo/count
     * 请求体(可选条件): 同 list 接口
     *
     * 响应数据: 数量（数字）
     *
     * 示例：
     * {
     *   "id": "req_task_006",
     *   "path": "/business/taskInfo/count",
     *   "body": "{\"status\":\"RUNNING\"}"
     * }
     */

    /**
     * 7) 启动任务（置为 RUNNING）
     *
     * 请求路径: /business/taskInfo/start
     * 请求体: { "id": 1 } 或 "1"
     *
     * 响应数据: 受影响行数
     *
     * 示例：
     * {
     *   "id": "req_task_007",
     *   "path": "/business/taskInfo/start",
     *   "body": "{\"id\":1}"
     * }
     */

    /**
     * 8) 暂停任务（置为 PAUSED）
     *
     * 请求路径: /business/taskInfo/pause
     * 请求体: { "id": 1 } 或 "1"
     *
     * 响应数据: 受影响行数
     *
     * 示例：
     * {
     *   "id": "req_task_008",
     *   "path": "/business/taskInfo/pause",
     *   "body": "1"
     * }
     */

    /**
     * 9) 恢复任务（PAUSED -> RUNNING）
     *
     * 请求路径: /business/taskInfo/resume
     * 请求体: { "id": 1 } 或 "1"
     *
     * 响应数据: 受影响行数
     *
     * 示例：
     * {
     *   "id": "req_task_009",
     *   "path": "/business/taskInfo/resume",
     *   "body": "1"
     * }
     */

    /**
     * 10) 完成任务（置为 COMPLETED）
     *
     * 请求路径: /business/taskInfo/complete
     * 请求体: { "id": 1 } 或 "1"
     *
     * 响应数据: 受影响行数
     *
     * 示例：
     * {
     *   "id": "req_task_010",
     *   "path": "/business/taskInfo/complete",
     *   "body": "1"
     * }
     */

    /**
     * 11) 标记任务失败（置为 ERROR）
     *
     * 请求路径: /business/taskInfo/fail
     * 请求体: { "id": 1 } 或 "1"
     *
     * 响应数据: 受影响行数
     *
     * 示例：
     * {
     *   "id": "req_task_011",
     *   "path": "/business/taskInfo/fail",
     *   "body": "1"
     * }
     */

    /**
     * ===============================================================
     * 任务设备关联表 (task_device_link) CRUD 接口
     * ===============================================================
     */

    /**
     * 12) 创建任务设备关联
     *
     * 请求路径: /business/taskInfo/link/create
     * 请求体: TaskDeviceLink 对象
     * {
     *   "taskId": 1,
     *   "deviceId": 2,
     *   "deviceName": "设备名称",
     *   "deviceFileConfigId": 10,
     *   "poolTemplateId": 5,
     *   "assignedQuantity": 1000,
     *   "status": "WAITING"
     * }
     *
     * 响应数据: 创建后的 TaskDeviceLink
     *
     * 示例：
     * {
     *   "id": "req_task_link_001",
     *   "path": "/business/taskInfo/link/create",
     *   "body": "{\"taskId\":1,\"deviceId\":2,\"assignedQuantity\":1000}"
     * }
     */

    /**
     * 13) 更新任务设备关联
     *
     * 请求路径: /business/taskInfo/link/update
     * 请求体: TaskDeviceLink 对象(必须包含 id)
     * {
     *   "id": 1,
     *   "status": "PRINTING",
     *   "completedQuantity": 500
     * }
     *
     * 响应数据: 更新后的 TaskDeviceLink
     *
     * 示例：
     * {
     *   "id": "req_task_link_002",
     *   "path": "/business/taskInfo/link/update",
     *   "body": "{\"id\":1,\"status\":\"PRINTING\",\"completedQuantity\":500}"
     * }
     */

    /**
     * 14) 删除任务设备关联（按任务ID）
     *
     * 请求路径: /business/taskInfo/link/delete
     * 请求体: { "id": 1 } 或 "1"
     *
     * 响应数据: 受影响行数
     *
     * 示例：
     * {
     *   "id": "req_task_link_003",
     *   "path": "/business/taskInfo/link/delete",
     *   "body": "{\"id\":1}"
     * }
     */

    /**
     * 15) 获取任务设备关联详情
     *
     * 请求路径: /business/taskInfo/link/get
     * 请求体: { "id": 1 } 或 "1"
     *
     * 响应数据: TaskDeviceLink
     *
     * 示例：
     * {
     *   "id": "req_task_link_004",
     *   "path": "/business/taskInfo/link/get",
     *   "body": "{\"id\":1}"
     * }
     */

    /**
     * 16) 查询任务设备关联列表
     *
     * 请求路径: /business/taskInfo/link/list
     * 请求体(可选条件):
     * {
     *   "taskId": 1,
     *   "deviceId": 2,
     *   "status": "PRINTING"
     * }
     *
     * 响应数据: [TaskDeviceLink]
     *
     * 示例：
     * {
     *   "id": "req_task_link_005",
     *   "path": "/business/taskInfo/link/list",
     *   "body": "{\"taskId\":1}"
     * }
     */

    /**
     * 17) 按任务ID查询关联列表
     *
     * 请求路径: /business/taskInfo/link/listByTask
     * 请求体: { "id": 1 } 或 "1"
     *
     * 响应数据: [TaskDeviceLink]
     *
     * 示例：
     * {
     *   "id": "req_task_link_006",
     *   "path": "/business/taskInfo/link/listByTask",
     *   "body": "1"
     * }
     */

    /**
     * 18) 按设备ID查询关联列表
     *
     * 请求路径: /business/taskInfo/link/listByDevice
     * 请求体: { "id": 1 } 或 "1"
     *
     * 响应数据: [TaskDeviceLink]
     *
     * 示例：
     * {
     *   "id": "req_task_link_007",
     *   "path": "/business/taskInfo/link/listByDevice",
     *   "body": "1"
     * }
     */

    /**
     * TaskDeviceLink 字段说明:
     * - id: 主键
     * - taskId: 所属任务ID
     * - deviceId: 关联的设备ID
     * - deviceName: 设备名称（冗余）
     * - deviceFileConfigId: 指定的打印文件配置ID
     * - poolTemplateId: 数据池模板ID
     * - status: 任务中的设备状态(WAITING/SENDING/PRINTING/COMPLETED/ERROR)
     * - assignedQuantity: 分配给设备的打印数量
     * - completedQuantity: 设备已完成数量
     * - createTime: 创建时间
     * - updateTime: 更新时间
     * - delFlag: 删除标记(0-正常,2-删除)
     */
}


