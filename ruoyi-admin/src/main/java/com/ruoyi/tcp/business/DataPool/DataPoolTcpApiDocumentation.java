package com.ruoyi.tcp.business.DataPool;

/**
 * 数据池管理TCP API文档
 * 
 * @author ruoyi
 */
public class DataPoolTcpApiDocumentation {
    
    /**
     * 数据池管理TCP API接口说明
     * 
     * 所有请求都应该使用以下JSON格式：
     * {
     *   "id": "请求ID",
     *   "path": "接口路径",
     *   "body": "请求体JSON字符串"
     * }
     * 
     * 响应格式：
     * {
     *   "id": "请求ID",
     *   "code": 200,
     *   "message": "响应消息",
     *   "data": "响应数据"
     * }
     */
    
    /**
     * 1. 查询数据池列表
     * 
     * 请求路径: /business/datapool/list
     * 请求体: DataPool对象 (可选字段用于查询条件)
     * {
     *   "poolName": "数据池名称(可选，模糊查询)",
     *   "sourceType": "数据源类型(可选)",
     *   "status": "运行状态(可选)"
     * }
     * 
     * 响应数据: {
     *   "dataPools": [数据池列表],
     *   "total": 总数
     * }
     */
    
    /**
     * 2. 获取数据池详情
     * 
     * 请求路径: /business/datapool/get
     * 请求体: {
     *   "id": "数据池ID"
     * }
     * 
     * 响应数据: {
     *   "dataPool": 数据池详情对象
     * }
     */
    
    /**
     * 3. 创建数据池
     * 
     * 请求路径: /business/datapool/create
     * 请求体: DataPool对象
     * {
     *   "poolName": "数据池名称(必填)",
     *   "sourceType": "数据源类型(必填)",
     *   "sourceConfigJson": "详细配置JSON(可选)",
     *   "parsingRuleJson": "解析规则JSON(可选)",
     *   "triggerConfigJson": "触发条件配置JSON(可选)",
     *   "remark": "备注(可选)"
     * }
     * 
     * 响应数据: {
     *   "id": "新创建的数据池ID"
     * }
     */
    
    /**
     * 4. 更新数据池
     * 
     * 请求路径: /business/datapool/update
     * 请求体: DataPool对象 (必须包含id字段)
     * {
     *   "id": "数据池ID(必填)",
     *   "poolName": "数据池名称(可选)",
     *   "sourceType": "数据源类型(可选)",
     *   "sourceConfigJson": "详细配置JSON(可选)",
     *   "parsingRuleJson": "解析规则JSON(可选)",
     *   "triggerConfigJson": "触发条件配置JSON(可选)",
     *   "remark": "备注(可选)"
     * }
     */
    
    /**
     * 5. 删除数据池
     * 
     * 请求路径: /business/datapool/delete
     * 请求体: {
     *   "id": "数据池ID"
     * }
     */
    
    /**
     * 6. 启动数据池
     * 
     * 请求路径: /business/datapool/start
     * 请求体: {
     *   "id": "数据池ID"
     * }
     */
    
    /**
     * 7. 停止数据池
     * 
     * 请求路径: /business/datapool/stop
     * 请求体: {
     *   "id": "数据池ID"
     * }
     */
    
    /**
     * 8. 更新数据池状态
     * 
     * 请求路径: /business/datapool/updateStatus
     * 请求体: {
     *   "id": "数据池ID",
     *   "status": "新状态(IDLE/RUNNING/WARNING/ERROR)"
     * }
     */
    
    /**
     * 9. 更新数据池计数
     * 
     * 请求路径: /business/datapool/updateCount
     * 请求体: {
     *   "id": "数据池ID",
     *   "totalCount": "总数据量(可选)",
     *   "pendingCount": "待打印数量(可选)"
     * }
     */
    
    /**
     * 数据源类型枚举值:
     * - U_DISK: U盘导入
     * - TCP_SERVER: TCP 服务端
     * - TCP_CLIENT: TCP 客户端
     * - HTTP: HTTP
     * - MQTT: MQTT
     * - WEBSOCKET: WebSocket
     * 
     * 运行状态枚举值:
     * - IDLE: 闲置
     * - RUNNING: 运行
     * - WARNING: 警告
     * - ERROR: 错误
     * 
     * 解析规则类型枚举值:
     * - JSON_PATH: JSON路径解析
     * - SUBSTRING: 子字符串解析
     * - DELIMITER: 分隔符解析
     * - JS_EXPRESSION: JavaScript表达式解析
     * 
     * 触发类型枚举值:
     * - BELOW_THRESHOLD: 低于阈值触发
     * - SCHEDULED: 定时触发
     */
    
    /**
     * 配置示例:
     * 
     * 1. U盘导入配置:
     * {
     *   "poolName": "U盘数据池",
     *   "sourceType": "U_DISK",
     *   "sourceConfigJson": "{\"filePath\":\"D:/data/products.xlsx\",\"sheetNameOrIndex\":\"Sheet1\",\"startRow\":2,\"dataColumn\":1,\"autoMonitor\":true}",
     *   "parsingRuleJson": null,
     *   "triggerConfigJson": null
     * }
     * 
     * 2. TCP服务端配置:
     * {
     *   "poolName": "TCP服务端数据池",
     *   "sourceType": "TCP_SERVER",
     *   "sourceConfigJson": "{\"ipAddress\":\"192.168.1.10\",\"port\":8899}",
     *   "parsingRuleJson": "{\"ruleType\":\"JSON_PATH\",\"expression\":\"$.data.code\"}",
     *   "triggerConfigJson": "{\"triggerType\":\"BELOW_THRESHOLD\",\"threshold\":100,\"requestCommand\":\"GET_DATA_NOW\"}"
     * }
     * 
     * 3. HTTP配置:
     * {
     *   "poolName": "HTTP数据池",
     *   "sourceType": "HTTP",
     *   "sourceConfigJson": "{\"url\":\"https://api.example.com/data\",\"method\":\"POST\",\"headers\":[{\"key\":\"Content-Type\",\"value\":\"application/json\"}],\"body\":\"{\\\"page\\\":1,\\\"size\\\":100}\"}",
     *   "parsingRuleJson": "{\"ruleType\":\"JSON_PATH\",\"expression\":\"$.data.list\"}",
     *   "triggerConfigJson": "{\"triggerType\":\"BELOW_THRESHOLD\",\"threshold\":100}"
     * }
     * 
     * 4. MQTT配置:
     * {
     *   "poolName": "MQTT数据池",
     *   "sourceType": "MQTT",
     *   "sourceConfigJson": "{\"brokerAddress\":\"mqtt.eclipse.org\",\"port\":1883,\"username\":\"user\",\"password\":\"password\",\"clientId\":\"uv_printer_system_1\"}",
     *   "parsingRuleJson": "{\"ruleType\":\"JSON_PATH\",\"expression\":\"$.data.code\"}",
     *   "triggerConfigJson": "{\"triggerType\":\"BELOW_THRESHOLD\",\"threshold\":100,\"subscribeTopic\":\"printer/data/response\",\"publishTopic\":\"printer/data/request\",\"requestPayload\":\"GET_DATA_NOW\"}"
     * }
     * 
     * 使用示例:
     * 
     * 1. 创建数据池:
     * {
     *   "id": "req001",
     *   "path": "/business/dataPool/create",
     *   "body": "{\"poolName\":\"测试数据池\",\"sourceType\":\"TCP_SERVER\",\"sourceConfigJson\":\"{\\\"ipAddress\\\":\\\"192.168.1.10\\\",\\\"port\\\":8899}\",\"parsingRuleJson\":\"{\\\"ruleType\\\":\\\"JSON_PATH\\\",\\\"expression\\\":\\\"$.data.code\\\"}\"}"
     * }
     * 
     * 2. 查询数据池列表:
     * {
     *   "id": "req002",
     *   "path": "/business/dataPool/list",
     *   "body": "{\"sourceType\":\"TCP_SERVER\"}"
     * }
     * 
     * 3. 更新数据池:
     * {
     *   "id": "req003",
     *   "path": "/business/dataPool/update",
     *   "body": "{\"id\":1,\"poolName\":\"更新后的数据池名称\",\"status\":\"RUNNING\"}"
     * }
     * 
     * 4. 启动数据池:
     * {
     *   "id": "req004",
     *   "path": "/business/dataPool/start",
     *   "body": "{\"id\":1}"
     * }
     */
}
