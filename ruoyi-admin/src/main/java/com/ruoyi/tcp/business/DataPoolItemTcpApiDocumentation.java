package com.ruoyi.tcp.business;

/**
 * 数据池热数据TCP API文档
 * 
 * 本文档详细说明了数据池热数据管理的TCP API接口规范
 * 
 * @author ruoyi
 */
public class DataPoolItemTcpApiDocumentation {

    /*
     * ================================
     * 数据池热数据管理 TCP API 文档
     * ================================
     * 
     * 基础说明：
     * - 所有请求都使用JSON格式
     * - 请求格式：{"path": "/business/dataPoolItem/xxx", "body": {...}, "id": "requestId"}
     * - 响应格式：{"code": 200, "message": "success", "data": {...}, "id": "requestId"}
     * - 成功响应code为200，失败响应code非200
     * 
     * 数据项状态说明：
     * - PENDING: 待打印
     * - PRINTING: 正在打印
     * - PRINTED: 打印成功
     * - FAILED: 打印失败
     * 
     * ================================
     * 1. 查询热数据列表
     * ================================
     * 
     * 路径: /business/dataPoolItem/list
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "poolId": 1,              // 可选，数据池ID
     *   "status": "PENDING",      // 可选，数据状态
     *   "deviceId": "device001"   // 可选，设备ID
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "查询热数据列表成功",
     *   "data": [
     *     {
     *       "id": 1,
     *       "poolId": 1,
     *       "itemData": "打印数据内容",
     *       "status": "PENDING",
     *       "printCount": 0,
     *       "deviceId": null,
     *       "receivedTime": "2024-01-01 10:00:00",
     *       "createTime": "2024-01-01 10:00:00",
     *       "updateTime": "2024-01-01 10:00:00"
     *     }
     *   ]
     * }
     * 
     * ================================
     * 2. 获取单个热数据
     * ================================
     * 
     * 路径: /business/dataPoolItem/get
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "id": 1                   // 必填，热数据ID
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "获取热数据成功",
     *   "data": {
     *     "id": 1,
     *     "poolId": 1,
     *     "itemData": "打印数据内容",
     *     "status": "PENDING",
     *     "printCount": 0,
     *     "lockId": null,
     *     "receivedTime": "2024-01-01 10:00:00"
     *   }
     * }
     * 
     * ================================
     * 3. 添加热数据
     * ================================
     * 
     * 路径: /business/dataPoolItem/add
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "poolId": 1,              // 必填，数据池ID
     *   "itemData": "打印数据内容" // 必填，数据内容
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "添加热数据成功",
     *   "data": {
     *     "id": 1,
     *     "poolId": 1,
     *     "itemData": "打印数据内容",
     *     "status": "PENDING",
     *     "printCount": 0
     *   }
     * }
     * 
     * ================================
     * 4. 批量添加热数据
     * ================================
     * 
     * 路径: /business/dataPoolItem/batchAdd
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "poolId": 1,              // 必填，数据池ID
     *   "itemDataList": [         // 必填，数据内容列表
     *     "打印数据1",
     *     "打印数据2",
     *     "打印数据3"
     *   ]
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "批量添加热数据成功",
     *   "data": {
     *     "addedCount": 3,
     *     "poolId": 1
     *   }
     * }
     * 
     * ================================
     * 5. 获取待打印数据
     * ================================
     * 
     * 路径: /business/dataPoolItem/getPending
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "poolId": 1,              // 可选，数据池ID
     *   "limit": 10               // 可选，限制数量
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "获取待打印数据成功",
     *   "data": [
     *     {
     *       "id": 1,
     *       "poolId": 1,
     *       "itemData": "打印数据内容",
     *       "status": "PENDING",
     *       "receivedTime": "2024-01-01 10:00:00"
     *     }
     *   ]
     * }
     * 
     * ================================
     * 6. 锁定数据项
     * ================================
     * 
     * 路径: /business/dataPoolItem/lock
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "id": 1,                  // 必填，数据项ID
     *   "deviceId": "device001"   // 必填，设备ID
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "锁定热数据成功"
     * }
     * 
     * ================================
     * 7. 批量锁定数据项
     * ================================
     * 
     * 路径: /business/dataPoolItem/batchLock
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "ids": [1, 2, 3],         // 必填，数据项ID列表
     *   "deviceId": "device001"   // 必填，设备ID
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "批量锁定热数据完成",
     *   "data": {
     *     "lockedCount": 2,       // 成功锁定的数量
     *     "totalCount": 3,        // 总请求数量
     *     "lockId": "device001"
     *   }
     * }
     * 
     * ================================
     * 8. 标记打印成功
     * ================================
     * 
     * 路径: /business/dataPoolItem/markPrinted
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "id": 1                   // 必填，数据项ID
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "标记打印成功"
     * }
     * 
     * ================================
     * 9. 标记打印失败
     * ================================
     * 
     * 路径: /business/dataPoolItem/markFailed
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "id": 1                   // 必填，数据项ID
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "标记打印失败"
     * }
     * 
     * ================================
     * 10. 释放锁定
     * ================================
     * 
     * 路径: /business/dataPoolItem/releaseLock
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "id": 1                   // 必填，数据项ID
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "释放锁定成功"
     * }
     * 
     * ================================
     * 11. 根据锁定ID释放锁定
     * ================================
     * 
     * 路径: /business/dataPoolItem/releaseLockByDeviceId
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "deviceId": "device001"   // 必填，设备ID
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "释放锁定成功",
     *   "data": {
     *     "releasedCount": 3,     // 释放的数据项数量
     *     "lockId": "device001"
     *   }
     * }
     * 
     * ================================
     * 12. 获取统计信息
     * ================================
     * 
     * 路径: /business/dataPoolItem/statistics
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "poolId": 1               // 可选，数据池ID
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "获取统计信息成功",
     *   "data": {
     *     "totalCount": 100,      // 总数据量
     *     "pendingCount": 50,     // 待打印数量
     *     "printingCount": 10,    // 正在打印数量
     *     "printedCount": 35,     // 已打印数量
     *     "failedCount": 5,       // 失败数量
     *     "poolId": 1
     *   }
     * }
     * 
     * ================================
     * 13. 获取队列信息
     * ================================
     * 
     * 路径: /business/dataPoolItem/queueInfo
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "poolId": 1               // 可选，数据池ID
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "获取队列信息成功",
     *   "data": {
     *     "totalCount": 100,
     *     "pendingCount": 50,
     *     "printingCount": 10,
     *     "printedCount": 35,
     *     "failedCount": 5,
     *     "earliestPendingTime": "2024-01-01 10:00:00",
     *     "activeDeviceIds": ["device001", "device002"],
     *     "activePrinterCount": 2,
     *     "poolId": 1
     *   }
     * }
     * 
     * ================================
     * 14. 重置失败数据
     * ================================
     * 
     * 路径: /business/dataPoolItem/resetFailed
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "poolId": 1               // 可选，数据池ID
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "重置失败数据成功",
     *   "data": {
     *     "resetCount": 5,        // 重置的数据项数量
     *     "poolId": 1
     *   }
     * }
     * 
     * ================================
     * 15. 清理已打印数据
     * ================================
     * 
     * 路径: /business/dataPoolItem/cleanPrinted
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "poolId": 1,              // 可选，数据池ID
     *   "beforeTime": "2024-01-01 00:00:00"  // 可选，时间限制
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "清理已打印数据成功",
     *   "data": {
     *     "cleanedCount": 20,     // 清理的数据项数量
     *     "poolId": 1
     *   }
     * }
     * 
     * ================================
     * 16. 更新热数据
     * ================================
     * 
     * 路径: /business/dataPoolItem/update
     * 方法: TCP请求
     * 
     * 请求体参数:
     * {
     *   "id": 1,                  // 必填，数据项ID
     *   "itemData": "新的数据内容", // 可选，更新数据内容
     *   "status": "FAILED",       // 可选，更新状态
     *   "printCount": 2           // 可选，更新打印次数
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "更新热数据成功"
     * }
     * 
     * ================================
     * 17. 删除热数据
     * ================================
     * 
     * 路径: /business/dataPoolItem/delete
     * 方法: TCP请求
     * 
     * 请求体参数（单个删除）:
     * {
     *   "id": 1                   // 必填，数据项ID
     * }
     * 
     * 请求体参数（批量删除）:
     * {
     *   "ids": [1, 2, 3]          // 必填，数据项ID列表
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "删除热数据成功"
     * }
     * 
     * ================================
     * 典型使用场景
     * ================================
     * 
     * 1. 数据源获取数据后添加到热数据表:
     *    /business/dataPoolItem/batchAdd
     * 
     * 2. 打印设备获取待打印数据:
     *    /business/dataPoolItem/getPending -> /business/dataPoolItem/batchLock
     * 
     * 3. 打印完成后标记状态:
     *    /business/dataPoolItem/markPrinted 或 /business/dataPoolItem/markFailed
     * 
     * 4. 设备断线后释放锁定:
     *    /business/dataPoolItem/releaseLockByDeviceId
     * 
     * 5. 监控打印队列状态:
     *    /business/dataPoolItem/queueInfo 或 /business/dataPoolItem/statistics
     * 
     * 6. 系统维护:
     *    /business/dataPoolItem/resetFailed -> /business/dataPoolItem/cleanPrinted
     * 
     * ================================
     * 错误代码说明
     * ================================
     * 
     * - 200: 操作成功
     * - 400: 请求参数错误
     * - 404: 数据不存在
     * - 409: 数据冲突（如已被锁定）
     * - 500: 服务器内部错误
     * 
     * ================================
     * 并发控制说明
     * ================================
     * 
     * 1. deviceId字段用于标识正在处理数据的设备
     * 2. 只有状态为PENDING的数据项可以被锁定
     * 3. 锁定后状态自动变为PRINTING
     * 4. 设备异常断线时应调用releaseLockByDeviceId释放所有锁定
     * 5. 建议deviceId使用设备唯一标识，如MAC地址或设备编号
     * 
     */
}
