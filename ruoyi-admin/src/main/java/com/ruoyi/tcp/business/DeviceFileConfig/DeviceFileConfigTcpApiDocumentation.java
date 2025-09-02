package com.ruoyi.tcp.business.DeviceFileConfig;

/**
 * 设备文件配置TCP API文档
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
public class DeviceFileConfigTcpApiDocumentation {

    /**
     * 1) 查询设备文件配置列表
     *
     * 请求路径: /business/deviceFileConfig/list
     * 请求体(可选条件):
     * {
     *   "deviceId": 1,
     *   "fileName": "模板文件名(可选，模糊查询)",
     *   "variableName": "变量名(可选，模糊查询)",
     *   "variableType": "变量类型(可选)",
     *   "fixedContent": "固定数据内容(可选，模糊查询)",
     *   "isDefault": 1,
     *   "description": "配置描述(可选，模糊查询)"
     * }
     *
     * 响应数据: [DeviceFileConfig]
     * DeviceFileConfig 字段：
     * - id: 主键
     * - deviceId: 所属设备ID
     * - fileName: 模板文件名称
     * - variableName: 变量名称
     * - variableType: 变量的数据类型(TEXT/NUMBER/DATE/SERIAL/QR_CODE)
     * - fixedContent: 固定数据内容
     * - isDefault: 是否为该设备默认配置(0:否, 1:是)
     * - description: 配置描述
     * - createBy: 创建者
     * - createTime: 创建时间
     * - updateBy: 更新者
     * - updateTime: 更新时间
     * - delFlag: 是否删除(0-未删,2-删除)
     *
     * 示例：
     * {
     *   "id": "req_dev_file_001",
     *   "path": "/business/deviceFileConfig/list",
     *   "body": "{\"deviceId\":1,\"variableType\":\"TEXT\"}"
     * }
     */

    /**
     * 2) 根据设备ID查询文件配置列表
     *
     * 请求路径: /business/deviceFileConfig/listByDeviceId
     * 请求体:
     * {
     *   "deviceId": 1
     * }
     *
     * 响应数据: [DeviceFileConfig]
     *
     * 示例：
     * {
     *   "id": "req_dev_file_002",
     *   "path": "/business/deviceFileConfig/listByDeviceId",
     *   "body": "{\"deviceId\":1}"
     * }
     */

    /**
     * 3) 根据设备ID和文件名查询文件配置列表
     *
     * 请求路径: /business/deviceFileConfig/listByDeviceIdAndFileName
     * 请求体:
     * {
     *   "deviceId": 1,
     *   "fileName": "product_label.prn"
     * }
     *
     * 响应数据: [DeviceFileConfig]
     *
     * 示例：
     * {
     *   "id": "req_dev_file_003",
     *   "path": "/business/deviceFileConfig/listByDeviceIdAndFileName",
     *   "body": "{\"deviceId\":1,\"fileName\":\"product_label.prn\"}"
     * }
     */

    /**
     * 4) 根据设备ID查询默认配置列表
     *
     * 请求路径: /business/deviceFileConfig/listDefaultByDeviceId
     * 请求体:
     * {
     *   "deviceId": 1
     * }
     *
     * 响应数据: [DeviceFileConfig]
     *
     * 示例：
     * {
     *   "id": "req_dev_file_004",
     *   "path": "/business/deviceFileConfig/listDefaultByDeviceId",
     *   "body": "{\"deviceId\":1}"
     * }
     */

    /**
     * 5) 根据变量类型查询文件配置列表
     *
     * 请求路径: /business/deviceFileConfig/listByVariableType
     * 请求体:
     * {
     *   "variableType": "TEXT"
     * }
     *
     * 响应数据: [DeviceFileConfig]
     *
     * 示例：
     * {
     *   "id": "req_dev_file_005",
     *   "path": "/business/deviceFileConfig/listByVariableType",
     *   "body": "{\"variableType\":\"TEXT\"}"
     * }
     */

    /**
     * 6) 获取设备文件配置详情
     *
     * 请求路径: /business/deviceFileConfig/get
     * 请求体:
     * {
     *   "id": 1
     * }
     *
     * 响应数据: DeviceFileConfig
     *
     * 示例：
     * {
     *   "id": "req_dev_file_006",
     *   "path": "/business/deviceFileConfig/get",
     *   "body": "{\"id\":1}"
     * }
     */

    /**
     * 7) 创建设备文件配置
     *
     * 请求路径: /business/deviceFileConfig/create
     * 请求体: DeviceFileConfig 对象(无需 id、createTime、updateTime、delFlag)
     * {
     *   "deviceId": 1,
     *   "fileName": "product_label.prn",
     *   "variableName": "PRODUCT_NAME",
     *   "variableType": "TEXT",
     *   "fixedContent": "",
     *   "isDefault": 0,
     *   "description": "产品名称变量配置"
     * }
     *
     * 响应数据: { "message": "创建设备文件配置成功" }
     *
     * 示例：
     * {
     *   "id": "req_dev_file_007",
     *   "path": "/business/deviceFileConfig/create",
     *   "body": "{\"deviceId\":1,\"fileName\":\"product_label.prn\",\"variableName\":\"PRODUCT_NAME\",\"variableType\":\"TEXT\",\"description\":\"产品名称变量配置\"}"
     * }
     */

    /**
     * 8) 更新设备文件配置
     *
     * 请求路径: /business/deviceFileConfig/update
     * 请求体: DeviceFileConfig 对象(必须包含 id)
     * {
     *   "id": 1,
     *   "variableName": "PRODUCT_NAME_UPDATED",
     *   "description": "更新后的产品名称变量配置"
     * }
     *
     * 响应数据: { "message": "更新设备文件配置成功" }
     *
     * 示例：
     * {
     *   "id": "req_dev_file_008",
     *   "path": "/business/deviceFileConfig/update",
     *   "body": "{\"id\":1,\"variableName\":\"PRODUCT_NAME_UPDATED\",\"description\":\"更新后的产品名称变量配置\"}"
     * }
     */

    /**
     * 9) 删除设备文件配置
     *
     * 请求路径: /business/deviceFileConfig/delete
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
     * 响应数据: { "message": "删除设备文件配置成功" } 或 { "message": "批量删除设备文件配置成功" }
     *
     * 示例：
     * 1) 单个删除
     * {
     *   "id": "req_dev_file_009",
     *   "path": "/business/deviceFileConfig/delete",
     *   "body": "{\"id\":1}"
     * }
     *
     * 2) 批量删除
     * {
     *   "id": "req_dev_file_010",
     *   "path": "/business/deviceFileConfig/delete",
     *   "body": "{\"ids\":[1,2,3]}"
     * }
     */

    /**
     * 10) 根据设备ID删除文件配置
     *
     * 请求路径: /business/deviceFileConfig/deleteByDeviceId
     * 请求体:
     * {
     *   "deviceId": 1
     * }
     *
     * 响应数据: { "message": "根据设备ID删除文件配置成功" }
     *
     * 示例：
     * {
     *   "id": "req_dev_file_011",
     *   "path": "/business/deviceFileConfig/deleteByDeviceId",
     *   "body": "{\"deviceId\":1}"
     * }
     */

    /**
     * 11) 根据设备ID和文件名删除文件配置
     *
     * 请求路径: /business/deviceFileConfig/deleteByDeviceIdAndFileName
     * 请求体:
     * {
     *   "deviceId": 1,
     *   "fileName": "product_label.prn"
     * }
     *
     * 响应数据: { "message": "根据设备ID和文件名删除文件配置成功" }
     *
     * 示例：
     * {
     *   "id": "req_dev_file_012",
     *   "path": "/business/deviceFileConfig/deleteByDeviceIdAndFileName",
     *   "body": "{\"deviceId\":1,\"fileName\":\"product_label.prn\"}"
     * }
     */

    /**
     * 12) 设置设备默认配置
     *
     * 请求路径: /business/deviceFileConfig/setDefault
     * 请求体:
     * {
     *   "deviceId": 1,
     *   "fileName": "product_label.prn"
     * }
     *
     * 响应数据: { "message": "设置设备默认配置成功" }
     *
     * 示例：
     * {
     *   "id": "req_dev_file_013",
     *   "path": "/business/deviceFileConfig/setDefault",
     *   "body": "{\"deviceId\":1,\"fileName\":\"product_label.prn\"}"
     * }
     */

    /**
     * 13) 统计设备文件配置数量
     *
     * 请求路径: /business/deviceFileConfig/count
     * 请求体(可选条件):
     * {
     *   "deviceId": 1,
     *   "fileName": "product_label.prn",
     *   "variableType": "TEXT",
     *   "isDefault": 1
     * }
     *
     * 响应数据: 配置数量(数字)
     *
     * 示例：
     * {
     *   "id": "req_dev_file_014",
     *   "path": "/business/deviceFileConfig/count",
     *   "body": "{\"deviceId\":1,\"variableType\":\"TEXT\"}"
     * }
     */

    /**
     * 14) 统计各设备文件配置数量
     *
     * 请求路径: /business/deviceFileConfig/countByDevice
     * 请求体: {}
     *
     * 响应数据: [{"deviceId": 1, "id": 5}, {"deviceId": 2, "id": 3}]
     *
     * 示例：
     * {
     *   "id": "req_dev_file_015",
     *   "path": "/business/deviceFileConfig/countByDevice",
     *   "body": "{}"
     * }
     */

    /**
     * 15) 批量创建设备文件配置
     *
     * 请求路径: /business/deviceFileConfig/batchCreate
     * 请求体: [DeviceFileConfig] 数组
     * [
     *   {
     *     "deviceId": 1,
     *     "fileName": "product_label.prn",
     *     "variableName": "PRODUCT_NAME",
     *     "variableType": "TEXT",
     *     "description": "产品名称变量"
     *   },
     *   {
     *     "deviceId": 1,
     *     "fileName": "product_label.prn",
     *     "variableName": "PRODUCT_CODE",
     *     "variableType": "SERIAL",
     *     "description": "产品编码变量"
     *   }
     * ]
     *
     * 响应数据: { "message": "批量创建设备文件配置成功" }
     *
     * 示例：
     * {
     *   "id": "req_dev_file_016",
     *   "path": "/business/deviceFileConfig/batchCreate",
     *   "body": "[{\"deviceId\":1,\"fileName\":\"product_label.prn\",\"variableName\":\"PRODUCT_NAME\",\"variableType\":\"TEXT\",\"description\":\"产品名称变量\"},{\"deviceId\":1,\"fileName\":\"product_label.prn\",\"variableName\":\"PRODUCT_CODE\",\"variableType\":\"SERIAL\",\"description\":\"产品编码变量\"}]"
     * }
     */

    /**
     * 16) 复制设备文件配置到其他设备
     *
     * 请求路径: /business/deviceFileConfig/copyToDevice
     * 请求体:
     * {
     *   "deviceId": 1,
     *   "id": 2
     * }
     * 注意：这里使用 id 字段存储目标设备ID，deviceId 字段存储源设备ID
     *
     * 响应数据: { "message": "复制设备文件配置成功，共复制X条配置" }
     *
     * 示例：
     * {
     *   "id": "req_dev_file_017",
     *   "path": "/business/deviceFileConfig/copyToDevice",
     *   "body": "{\"deviceId\":1,\"id\":2}"
     * }
     */

    /**
     * 变量类型枚举值:
     * - TEXT: 文本
     * - NUMBER: 数字
     * - DATE: 日期
     * - SERIAL: 序列号
     * - QR_CODE: 二维码
     * 
     * 配置示例:
     * 
     * 1. 文本变量配置:
     * {
     *   "deviceId": 1,
     *   "fileName": "product_label.prn",
     *   "variableName": "PRODUCT_NAME",
     *   "variableType": "TEXT",
     *   "fixedContent": "",
     *   "isDefault": 0,
     *   "description": "产品名称变量，从数据源动态获取"
     * }
     * 
     * 2. 固定数据配置:
     * {
     *   "deviceId": 1,
     *   "fileName": "company_label.prn",
     *   "variableName": "FIXED_COMPANY_NAME",
     *   "variableType": "TEXT",
     *   "fixedContent": "某某科技有限公司",
     *   "isDefault": 1,
     *   "description": "公司名称固定数据"
     * }
     * 
     * 3. 序列号变量配置:
     * {
     *   "deviceId": 1,
     *   "fileName": "serial_label.prn",
     *   "variableName": "PRODUCT_SERIAL",
     *   "variableType": "SERIAL",
     *   "fixedContent": "",
     *   "isDefault": 0,
     *   "description": "产品序列号，自动递增"
     * }
     * 
     * 4. 二维码变量配置:
     * {
     *   "deviceId": 1,
     *   "fileName": "qr_label.prn",
     *   "variableName": "PRODUCT_QR",
     *   "variableType": "QR_CODE",
     *   "fixedContent": "",
     *   "isDefault": 0,
     *   "description": "产品二维码，包含产品信息"
     * }
     * 
     * 使用示例:
     * 
     * 1. 创建设备文件配置:
     * {
     *   "id": "req_dev_file_001",
     *   "path": "/business/deviceFileConfig/create",
     *   "body": "{\"deviceId\":1,\"fileName\":\"product_label.prn\",\"variableName\":\"PRODUCT_NAME\",\"variableType\":\"TEXT\",\"description\":\"产品名称变量配置\"}"
     * }
     * 
     * 2. 查询设备的文件配置:
     * {
     *   "id": "req_dev_file_002",
     *   "path": "/business/deviceFileConfig/listByDeviceId",
     *   "body": "{\"deviceId\":1}"
     * }
     * 
     * 3. 设置默认配置:
     * {
     *   "id": "req_dev_file_003",
     *   "path": "/business/deviceFileConfig/setDefault",
     *   "body": "{\"deviceId\":1,\"fileName\":\"product_label.prn\"}"
     * }
     * 
     * 4. 复制配置到其他设备:
     * {
     *   "id": "req_dev_file_004",
     *   "path": "/business/deviceFileConfig/copyToDevice",
     *   "body": "{\"deviceId\":1,\"id\":2}"
     * }
     */
}
