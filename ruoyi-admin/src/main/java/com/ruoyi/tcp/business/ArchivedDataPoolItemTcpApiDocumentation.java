package com.ruoyi.tcp.business;

/**
 * 归档数据池项目 TCP API 文档
 * 
 * 归档数据表用于存储已完成打印的热数据项，提供数据追溯、统计分析等功能
 * 
 * @author ruoyi
 */
public class ArchivedDataPoolItemTcpApiDocumentation {
    
    /**
     * 归档数据表字段说明：
     * 
     * - id: 主键ID (直接使用原热表中的ID，保持数据一致性)
     * - item_data: 打印的数据内容
     * - final_status: 最终状态 (PRINTED 或 FAILED)
     * - print_count: 最终打印次数
     * - pool_id: 所属数据池ID
     * - pool_name: 数据池名称 (冗余字段，便于查询)
     * - device_id: 执行打印的设备ID或名称
     * - received_time: 数据进入系统的时间
     * - printed_time: 打印完成的时间戳
     * - archived_time: 数据归档时间
     * - verification_data: 扫描设备回传的原始数据
     * - verification_status: 校验结果状态
     * 
     * 校验状态枚举值：
     * - SUCCESS: 校验成功
     * - FAIL: 校验失败
     * - PROCESSING: 校验中
     * - TIMEOUT: 校验超时
     * - NOT_REQUIRED: 无需校验
     */
    
    /**
     * 1. 查询归档数据列表
     * 
     * 路径: /business/archivedDataPoolItem/list
     * 方法: POST
     * 请求体: ArchivedDataPoolItem 对象 (支持条件查询)
     * 
     * 示例请求:
     * {
     *   "id": "1234567890",
     *   "path": "/business/archivedDataPoolItem/list",
     *   "body": "{\"poolId\": 1, \"finalStatus\": \"PRINTED\"}"
     * }
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "查询归档数据列表成功",
     *   "data": [
     *     {
     *       "id": 1001,
     *       "itemData": "产品A-001",
     *       "finalStatus": "PRINTED",
     *       "printCount": 1,
     *       "poolId": 1,
     *       "poolName": "产品数据池",
     *       "deviceId": "PRINTER_001",
     *       "receivedTime": "2024-01-01 10:00:00",
     *       "printedTime": "2024-01-01 10:05:00",
     *       "archivedTime": "2024-01-01 10:06:00",
     *       "verificationData": "扫描结果数据",
     *       "verificationStatus": "SUCCESS"
     *     }
     *   ]
     * }
     */
    
    /**
     * 2. 获取单个归档数据
     * 
     * 路径: /business/archivedDataPoolItem/get
     * 方法: POST
     * 请求体: {"id": 1001}
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "获取归档数据成功",
     *   "data": {
     *     "id": 1001,
     *     "itemData": "产品A-001",
     *     "finalStatus": "PRINTED",
     *     "printCount": 1,
     *     "poolId": 1,
     *     "poolName": "产品数据池",
     *     "deviceId": "PRINTER_001",
     *     "receivedTime": "2024-01-01 10:00:00",
     *     "printedTime": "2024-01-01 10:05:00",
     *     "archivedTime": "2024-01-01 10:06:00",
     *     "verificationData": "扫描结果数据",
     *     "verificationStatus": "SUCCESS"
     *   }
     * }
     */
    
    /**
     * 3. 更新归档数据
     * 
     * 路径: /business/archivedDataPoolItem/update
     * 方法: POST
     * 请求体: ArchivedDataPoolItem 对象 (需要包含id)
     * 
     * 示例请求:
     * {
     *   "id": "1234567890",
     *   "path": "/business/archivedDataPoolItem/update",
     *   "body": "{\"id\": 1001, \"verificationData\": \"新的校验数据\", \"verificationStatus\": \"SUCCESS\"}"
     * }
     */
    
    /**
     * 4. 删除归档数据
     * 
     * 路径: /business/archivedDataPoolItem/delete
     * 方法: POST
     * 请求体: {"id": 1001} 或 {"ids": [1001, 1002, 1003]}
     * 
     * 支持单个删除和批量删除
     */
    
    /**
     * 5. 根据数据池ID查询归档数据
     * 
     * 路径: /business/archivedDataPoolItem/getByPoolId
     * 方法: POST
     * 请求体: {"poolId": 1}
     * 
     * 返回指定数据池的所有归档数据
     */
    
    /**
     * 6. 根据时间范围查询归档数据
     * 
     * 路径: /business/archivedDataPoolItem/getByTimeRange
     * 方法: POST
     * 请求体: {"poolId": 1, "startTime": "2024-01-01 00:00:00", "endTime": "2024-01-31 23:59:59"}
     * 
     * poolId 为可选参数，不传则查询所有数据池
     */
    
    /**
     * 7. 根据状态查询归档数据
     * 
     * 路径: /business/archivedDataPoolItem/getByStatus
     * 方法: POST
     * 请求体: {"poolId": 1, "finalStatus": "PRINTED"}
     * 
     * finalStatus 可选值: PRINTED, FAILED
     */
    
    /**
     * 8. 根据设备ID查询归档数据
     * 
     * 路径: /business/archivedDataPoolItem/getByDeviceId
     * 方法: POST
     * 请求体: {"deviceId": "PRINTER_001"}
     * 
     * 查询指定设备处理的所有归档数据
     */
    
    /**
     * 9. 根据校验状态查询归档数据
     * 
     * 路径: /business/archivedDataPoolItem/getByVerificationStatus
     * 方法: POST
     * 请求体: {"poolId": 1, "verificationStatus": "SUCCESS"}
     * 
     * verificationStatus 可选值: SUCCESS, FAIL, PROCESSING, TIMEOUT, NOT_REQUIRED
     */
    
    /**
     * 10. 获取归档数据统计信息
     * 
     * 路径: /business/archivedDataPoolItem/statistics
     * 方法: POST
     * 请求体: {"poolId": 1} 或 {}
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "获取统计信息成功",
     *   "data": {
     *     "totalCount": 1000,
     *     "printedCount": 950,
     *     "failedCount": 50,
     *     "successVerificationCount": 900,
     *     "failVerificationCount": 100
     *   }
     * }
     */
    
    /**
     * 11. 清理指定时间之前的归档数据
     * 
     * 路径: /business/archivedDataPoolItem/cleanBeforeTime
     * 方法: POST
     * 请求体: {"poolId": 1, "beforeTime": "2023-01-01 00:00:00"}
     * 
     * 清理指定时间之前的数据，释放存储空间
     * poolId 为可选参数，不传则清理所有数据池
     */
    
    /**
     * 12. 根据打印状态归档数据
     * 
     * 路径: /business/archivedDataPoolItem/archiveByPrintStatus
     * 方法: POST
     * 请求体: {"poolId": 1} 或 {}
     * 
     * 自动归档所有已打印或失败的热数据项
     * poolId 为可选参数，不传则处理所有数据池
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "归档数据成功",
     *   "data": {
     *     "archivedCount": 150,
     *     "poolId": 1
     *   }
     * }
     */
    
    /**
     * 13. 更新校验信息
     * 
     * 路径: /business/archivedDataPoolItem/updateVerificationInfo
     * 方法: POST
     * 请求体: {"id": 1001, "verificationData": "扫描结果", "verificationStatus": "SUCCESS"}
     * 
     * 更新指定归档数据的校验信息和状态
     */
    
    /**
     * 14. 根据设备ID批量更新校验状态
     * 
     * 路径: /business/archivedDataPoolItem/updateVerificationStatusByDeviceId
     * 方法: POST
     * 请求体: {"deviceId": "PRINTER_001", "verificationStatus": "SUCCESS"}
     * 
     * 批量更新指定设备的所有归档数据的校验状态
     * 
     * 响应示例:
     * {
     *   "code": 200,
     *   "message": "批量更新校验状态成功",
     *   "data": {
     *     "updateCount": 25,
     *     "deviceId": "PRINTER_001"
     *   }
     * }
     */
    
    /**
     * 15. 获取导出数据
     * 
     * 路径: /business/archivedDataPoolItem/exportData
     * 方法: POST
     * 请求体: {"poolId": 1, "startTime": "2024-01-01 00:00:00", "endTime": "2024-01-31 23:59:59"}
     * 
     * 获取指定时间范围内的归档数据，用于数据导出和分析
     */
    
    /**
     * 使用场景说明：
     * 
     * 1. 数据追溯：通过归档数据可以追溯每个打印任务的完整生命周期
     * 2. 质量分析：分析打印失败的原因，优化打印流程
     * 3. 设备监控：监控各设备的打印质量和效率
     * 4. 数据统计：生成打印报表，分析业务趋势
     * 5. 合规要求：满足数据保留和审计要求
     * 
     * 注意事项：
     * 
     * 1. 归档数据使用原热数据表的ID，确保数据一致性
     * 2. 支持软删除，删除的数据标记为已删除状态
     * 3. 提供多种查询方式，满足不同的业务需求
     * 4. 支持批量操作，提高处理效率
     * 5. 包含完整的审计信息，便于问题排查
     */
}
