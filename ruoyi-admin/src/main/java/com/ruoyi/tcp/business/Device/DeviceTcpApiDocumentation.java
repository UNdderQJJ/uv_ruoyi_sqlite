package com.ruoyi.tcp.business.Device;

/**
 * 设备管理TCP API文档
 * 
 * 本文档参照 {@link com.ruoyi.tcp.business.DataPool.DataPoolTcpApiDocumentation} 的风格与结构。
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
public class DeviceTcpApiDocumentation {

    /**
     * 1) 查询设备列表
     *
     * 请求路径: /business/device/list
     * 请求体(可选条件):
     * {
     *   "deviceUuid": "设备UUID(可选，精确查询)",
     *   "name": "设备名称(可选，模糊查询)",
     *   "deviceType": "设备类型(可选)",
     *   "model": "设备型号(可选，模糊查询)",
     *   "location": "设备位置(可选，模糊查询)",
     *   "connectionType": "连接类型(可选)",
     *   "ipAddress": "IP地址(可选，精确查询)",
     *   "port": 9100,
     *   "status": "设备状态(可选)",
     *   "isEnabled": 1,
     *   "description": "设备备注(可选，模糊查询)"
     * }
     *
     * 响应数据: [DeviceInfo]
     * DeviceInfo 字段：
     * - id: 主键
     * - deviceUuid: 设备唯一标识符
     * - name: 设备名称
     * - deviceType: 设备类型(PRINTER/CODER/SCANNER)
     * - model: 设备型号
     * - location: 设备物理位置描述
     * - connectionType: 连接类型(TCP/SERIAL)
     * - ipAddress: IP地址
     * - port: 端口号
     * - serialPort: 串口号
     * - baudRate: 波特率
     * - dataBits: 数据位
     * - stopBits: 停止位
     * - parity: 校验位(NONE/ODD/EVEN/MARK/SPACE)
     * - status: 设备状态(OFFLINE/ONLINE_IDLE/ONLINE_PRINTING/ONLINE_SCANNING/ERROR/MAINTENANCE)
     * - currentTaskId: 当前正在执行的任务ID
     * - isEnabled: 是否启用(0:禁用, 1:启用)
     * - lastHeartbeatTime: 最后心跳时间
     * - parameters: 设备特定参数(JSON格式)
     * - description: 设备备注信息
     * - createBy: 创建者
     * - createTime: 创建时间
     * - updateBy: 更新者
     * - updateTime: 更新时间
     * - delFlag: 是否删除(0-未删,2-删除)
     *
     * 示例：
     * {
     *   "id": "req_dev_001",
     *   "path": "/business/device/list",
     *   "body": "{\"deviceType\":\"PRINTER\",\"status\":\"ONLINE_IDLE\"}"
     * }
     */

    /**
     * 2) 根据设备类型查询设备列表
     *
     * 请求路径: /business/device/listByType
     * 请求体:
     * {
     *   "deviceType": "PRINTER"
     * }
     *
     * 响应数据: [DeviceInfo]
     *
     * 示例：
     * {
     *   "id": "req_dev_002",
     *   "path": "/business/device/listByType",
     *   "body": "{\"deviceType\":\"PRINTER\"}"
     * }
     */

    /**
     * 3) 根据设备状态查询设备列表
     *
     * 请求路径: /business/device/listByStatus
     * 请求体:
     * {
     *   "status": "ONLINE_IDLE"
     * }
     *
     * 响应数据: [DeviceInfo]
     *
     * 示例：
     * {
     *   "id": "req_dev_003",
     *   "path": "/business/device/listByStatus",
     *   "body": "{\"status\":\"ONLINE_IDLE\"}"
     * }
     */

    /**
     * 4) 查询启用的设备列表
     *
     * 请求路径: /business/device/listEnabled
     * 请求体: {}
     *
     * 响应数据: [DeviceInfo]
     *
     * 示例：
     * {
     *   "id": "req_dev_004",
     *   "path": "/business/device/listEnabled",
     *   "body": "{}"
     * }
     */

    /**
     * 5) 查询在线设备列表
     *
     * 请求路径: /business/device/listOnline
     * 请求体: {}
     *
     * 响应数据: [DeviceInfo]
     *
     * 示例：
     * {
     *   "id": "req_dev_005",
     *   "path": "/business/device/listOnline",
     *   "body": "{}"
     * }
     */

    /**
     * 6) 获取设备详情
     *
     * 请求路径: /business/device/get
     * 请求体:
     * {
     *   "id": 1
     * }
     *
     * 响应数据: DeviceInfo
     *
     * 示例：
     * {
     *   "id": "req_dev_006",
     *   "path": "/business/device/get",
     *   "body": "{\"id\":1}"
     * }
     */

    /**
     * 7) 根据设备UUID获取设备详情
     *
     * 请求路径: /business/device/getByUuid
     * 请求体:
     * {
     *   "deviceUuid": "UV-Printer-001"
     * }
     *
     * 响应数据: DeviceInfo
     *
     * 示例：
     * {
     *   "id": "req_dev_007",
     *   "path": "/business/device/getByUuid",
     *   "body": "{\"deviceUuid\":\"UV-Printer-001\"}"
     * }
     */

    /**
     * 8) 创建设备
     *
     * 请求路径: /business/device/create
     * 请求体: DeviceInfo 对象(无需 id、createTime、updateTime、delFlag)
     * {
     *   "deviceUuid": "UV-Printer-001",
     *   "name": "一号车间UV打印机A",
     *   "deviceType": "PRINTER",
     *   "model": "UV-3000",
     *   "location": "一号车间",
     *   "connectionType": "TCP",
     *   "ipAddress": "192.168.1.101",
     *   "port": 9100,
     *   "status": "OFFLINE",
     *   "isEnabled": 1,
     *   "parameters": "{\"printSpeed\":150,\"laserPower\":80,\"resolution\":600}",
     *   "description": "主要打印标签"
     * }
     *
     * 响应数据: { "message": "创建设备成功" }
     *
     * 示例：
     * {
     *   "id": "req_dev_008",
     *   "path": "/business/device/create",
     *   "body": "{\"deviceUuid\":\"UV-Printer-001\",\"name\":\"一号车间UV打印机A\",\"deviceType\":\"PRINTER\",\"model\":\"UV-3000\",\"location\":\"一号车间\",\"connectionType\":\"TCP\",\"ipAddress\":\"192.168.1.101\",\"port\":9100,\"status\":\"OFFLINE\",\"isEnabled\":1,\"parameters\":\"{\\\"printSpeed\\\":150,\\\"laserPower\\\":80,\\\"resolution\\\":600}\",\"description\":\"主要打印标签\"}"
     * }
     */

    /**
     * 9) 更新设备
     *
     * 请求路径: /business/device/update
     * 请求体: DeviceInfo 对象(必须包含 id)
     * {
     *   "id": 1,
     *   "name": "一号车间UV打印机A-更新",
     *   "ipAddress": "192.168.1.102",
     *   "port": 9101
     * }
     *
     * 响应数据: { "message": "更新设备成功" }
     *
     * 示例：
     * {
     *   "id": "req_dev_009",
     *   "path": "/business/device/update",
     *   "body": "{\"id\":1,\"name\":\"一号车间UV打印机A-更新\",\"ipAddress\":\"192.168.1.102\",\"port\":9101}"
     * }
     */

    /**
     * 10) 删除设备
     *
     * 请求路径: /business/device/delete
     *
     * 方式一(单个):
     * {
     *   "id": 1
     * }
     *
     * 方式二(批量):
     * {
     *   "ids": [1, 2, 3]
     * }
     *
     * 响应数据: { "message": "删除设备成功" } 或 { "message": "批量删除设备成功" }
     *
     * 示例：
     * 1) 单个删除
     * {
     *   "id": "req_dev_010",
     *   "path": "/business/device/delete",
     *   "body": "{\"id\":1}"
     * }
     *
     * 2) 批量删除
     * {
     *   "id": "req_dev_011",
     *   "path": "/business/device/delete",
     *   "body": "{\"ids\":[1,2,3]}"
     * }
     */

    /**
     * 11) 更新设备状态
     *
     * 请求路径: /business/device/updateStatus
     * 请求体:
     * {
     *   "id": 1,
     *   "status": "ONLINE_PRINTING"
     * }
     *
     * 响应数据: { "message": "更新设备状态成功" }
     *
     * 示例：
     * {
     *   "id": "req_dev_012",
     *   "path": "/business/device/updateStatus",
     *   "body": "{\"id\":1,\"status\":\"ONLINE_PRINTING\"}"
     * }
     */

    /**
     * 12) 更新设备心跳时间
     *
     * 请求路径: /business/device/updateHeartbeat
     * 请求体:
     * {
     *   "id": 1
     * }
     *
     * 响应数据: { "message": "更新设备心跳成功" }
     *
     * 示例：
     * {
     *   "id": "req_dev_013",
     *   "path": "/business/device/updateHeartbeat",
     *   "body": "{\"id\":1}"
     * }
     */

    /**
     * 13) 更新设备当前任务
     *
     * 请求路径: /business/device/updateCurrentTask
     * 请求体:
     * {
     *   "id": 1,
     *   "taskId": 100
     * }
     *
     * 响应数据: { "message": "更新设备当前任务成功" }
     *
     * 示例：
     * {
     *   "id": "req_dev_014",
     *   "path": "/business/device/updateCurrentTask",
     *   "body": "{\"id\":1,\"taskId\":100}"
     * }
     */

    /**
     * 14) 启用/禁用设备
     *
     * 请求路径: /business/device/enable
     * 请求体:
     * {
     *   "id": 1,
     *   "isEnabled": 0
     * }
     *
     * 响应数据: { "message": "更新设备启用状态成功" }
     *
     * 示例：
     * {
     *   "id": "req_dev_015",
     *   "path": "/business/device/enable",
     *   "body": "{\"id\":1,\"isEnabled\":0}"
     * }
     */

    /**
     * 15) 批量启用/禁用设备
     *
     * 请求路径: /business/device/batchEnable
     * 请求体:
     * {
     *   "ids": [1, 2, 3],
     *   "isEnabled": 1
     * }
     *
     * 响应数据: { "message": "批量更新设备启用状态成功" }
     *
     * 示例：
     * {
     *   "id": "req_dev_016",
     *   "path": "/business/device/batchEnable",
     *   "body": "{\"ids\":[1,2,3],\"isEnabled\":1}"
     * }
     */

    /**
     * 16) 统计设备数量
     *
     * 请求路径: /business/device/count
     * 请求体(可选条件):
     * {
     *   "deviceType": "PRINTER",
     *   "status": "ONLINE_IDLE",
     *   "isEnabled": 1
     * }
     *
     * 响应数据: 设备数量(数字)
     *
     * 示例：
     * {
     *   "id": "req_dev_017",
     *   "path": "/business/device/count",
     *   "body": "{\"deviceType\":\"PRINTER\",\"status\":\"ONLINE_IDLE\"}"
     * }
     */

    /**
     * 17) 统计各类型设备数量
     *
     * 请求路径: /business/device/countByType
     * 请求体: {}
     *
     * 响应数据: [{"deviceType": "PRINTER", "id": 5}, {"deviceType": "CODER", "id": 1}]
     *
     * 示例：
     * {
     *   "id": "req_dev_018",
     *   "path": "/business/device/countByType",
     *   "body": "{}"
     * }
     */

    /**
     * 设备类型枚举值:
     * - PRINTER: 打印机
     * - CODER: 喷码机
     * - SCANNER: 扫码枪
     * 
     * 连接类型枚举值:
     * - TCP: TCP网络
     * - SERIAL: 串口
     * 
     * 设备状态枚举值:
     * - OFFLINE: 离线
     * - ONLINE_IDLE: 在线空闲
     * - ONLINE_PRINTING: 在线打印
     * - ONLINE_SCANNING: 在线扫描
     * - ERROR: 故障
     * - MAINTENANCE: 维护
     * 
     * 校验位枚举值:
     * - NONE: 无校验
     * - ODD: 奇校验
     * - EVEN: 偶校验
     * - MARK: 标记校验
     * - SPACE: 空格校验
     */

    /**
     * 配置示例:
     * 
     * 1. TCP网络打印机配置:
     * {
     *   "deviceUuid": "UV-Printer-001",
     *   "name": "一号车间UV打印机A",
     *   "deviceType": "PRINTER",
     *   "model": "UV-3000",
     *   "location": "一号车间",
     *   "connectionType": "TCP",
     *   "ipAddress": "192.168.1.101",
     *   "port": 9100,
     *   "status": "OFFLINE",
     *   "isEnabled": 1,
     *   "parameters": "{\"printSpeed\":150,\"laserPower\":80,\"resolution\":600}",
     *   "description": "主要打印标签"
     * }
     * 
     * 2. 串口喷码机配置:
     * {
     *   "deviceUuid": "CODER-001",
     *   "name": "二号车间喷码机",
     *   "deviceType": "CODER",
     *   "model": "Coder-2000",
     *   "location": "二号车间",
     *   "connectionType": "SERIAL",
     *   "serialPort": "COM1",
     *   "baudRate": 9600,
     *   "dataBits": 8,
     *   "stopBits": 1,
     *   "parity": "NONE",
     *   "status": "OFFLINE",
     *   "isEnabled": 1,
     *   "parameters": "{\"printHeight\":10,\"printWidth\":20}",
     *   "description": "用于产品喷码"
     * }
     * 
     * 3. 扫码枪配置:
     * {
     *   "deviceUuid": "SCANNER-001",
     *   "name": "质检扫码枪",
     *   "deviceType": "SCANNER",
     *   "model": "Scanner-1000",
     *   "location": "质检台",
     *   "connectionType": "SERIAL",
     *   "serialPort": "/dev/ttyS0",
     *   "baudRate": 115200,
     *   "dataBits": 8,
     *   "stopBits": 1,
     *   "parity": "NONE",
     *   "status": "OFFLINE",
     *   "isEnabled": 1,
     *   "parameters": "{\"scanMode\":\"CONTINUOUS\",\"beepEnabled\":true}",
     *   "description": "用于质检扫码"
     * }
     * 
     * 使用示例:
     * 
     * 1. 创建设备:
     * {
     *   "id": "req_dev_001",
     *   "path": "/business/device/create",
     *   "body": "{\"deviceUuid\":\"UV-Printer-001\",\"name\":\"一号车间UV打印机A\",\"deviceType\":\"PRINTER\",\"connectionType\":\"TCP\",\"ipAddress\":\"192.168.1.101\",\"port\":9100}"
     * }
     * 
     * 2. 查询打印机列表:
     * {
     *   "id": "req_dev_002",
     *   "path": "/business/device/listByType",
     *   "body": "{\"deviceType\":\"PRINTER\"}"
     * }
     * 
     * 3. 更新设备状态:
     * {
     *   "id": "req_dev_003",
     *   "path": "/business/device/updateStatus",
     *   "body": "{\"id\":1,\"status\":\"ONLINE_PRINTING\"}"
     * }
     * 
     * 4. 统计在线设备数量:
     * {
     *   "id": "req_dev_004",
     *   "path": "/business/device/count",
     *   "body": "{\"status\":\"ONLINE_IDLE\"}"
     * }
     */
}
