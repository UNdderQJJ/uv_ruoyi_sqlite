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
     * 响应数据: 创建后的 TaskInfo
     *
     * 示例：
     * {
     *   "id": "req_task_001",
     *   "path": "/business/taskInfo/create",
     *   "body": "{\"name\":\"任务1\",\"poolId\":1,\"plannedQuantity\":-1}"
     * }
     */

    /**
     * 2) 更新任务
     *
     * 请求路径: /business/taskInfo/update
     * 请求体: TaskInfo 对象(必须包含 id)
     * {
     *   "id": 1,
     *   "name": "任务名称-更新",
     *   "plannedQuantity": 1000
     * }
     *
     * 响应数据: 更新后的 TaskInfo
     *
     * 示例：
     * {
     *   "id": "req_task_002",
     *   "path": "/business/taskInfo/update",
     *   "body": "{\"id\":1,\"name\":\"任务名称-更新\"}"
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
}


