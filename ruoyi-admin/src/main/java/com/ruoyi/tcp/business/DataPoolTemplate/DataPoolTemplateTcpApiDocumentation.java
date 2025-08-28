package com.ruoyi.tcp.business.DataPoolTemplate;

/**
 * 数据池模板管理TCP API文档
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
public class DataPoolTemplateTcpApiDocumentation {

    /**
     * 1) 查询模板列表
     *
     * 请求路径: /business/dataPoolTemplate/list
     * 请求体(可选条件):
     * {
     *   "poolId": 1,                // 可选，按数据池过滤
     *   "tempName": "标签"          // 可选，名称模糊查询
     * }
     *
     * 响应数据: [DataPoolTemplate]
     * DataPoolTemplate 字段：
     * - id: 主键
     * - poolId: 数据池ID
     * - tempName: 模板名称
     * - tempContent: 模板内容(字符串)
     * - xAxis: x轴
     * - yAxis: y轴
     * - angle: 旋转角度
     * - width: 宽度
     * - height: 高度
     * - delFlag: 是否删除(0-未删,2-删除)
     * - createTime: 创建时间
     * - updateTime: 更新时间
     *
     * 示例：
     * {
     *   "id": "req_tpl_001",
     *   "path": "/business/dataPoolTemplate/list",
     *   "body": "{\"poolId\":1,\"tempName\":\"标签\"}"
     * }
     */

    /**
     * 2) 按数据池查询模板列表
     *
     * 请求路径: /business/dataPoolTemplate/listByPool
     * 请求体:
     * {
     *   "poolId": 1
     * }
     *
     * 响应数据: [DataPoolTemplate]
     *
     * 示例：
     * {
     *   "id": "req_tpl_002",
     *   "path": "/business/dataPoolTemplate/listByPool",
     *   "body": "{\"poolId\":1}"
     * }
     */

    /**
     * 3) 获取模板详情
     *
     * 请求路径: /business/dataPoolTemplate/get
     * 请求体:
     * {
     *   "id": 10
     * }
     *
     * 响应数据: DataPoolTemplate
     *
     * 示例：
     * {
     *   "id": "req_tpl_003",
     *   "path": "/business/dataPoolTemplate/get",
     *   "body": "{\"id\":10}"
     * }
     */

    /**
     * 4) 创建模板
     *
     * 请求路径: /business/dataPoolTemplate/create
     * 请求体: DataPoolTemplate 对象(无需 id、delFlag、createTime、updateTime)
     * {
     *   "poolId": 1,
     *   "tempName": "标签A",
     *   "tempContent": "{...}",
     *   "xAxis": 10,
     *   "yAxis": 20,
     *   "angle": 0,
     *   "width": 200,
     *   "height": 100
     * }
     *
     * 响应数据: { "rows": 1 }
     *
     * 示例：
     * {
     *   "id": "req_tpl_004",
     *   "path": "/business/dataPoolTemplate/create",
     *   "body": "{\"poolId\":1,\"tempName\":\"标签A\",\"tempContent\":\"{...}\",\"xAxis\":10,\"yAxis\":20,\"angle\":0,\"width\":200,\"height\":100}"
     * }
     */

    /**
     * 5) 更新模板
     *
     * 请求路径: /business/dataPoolTemplate/update
     * 请求体: DataPoolTemplate 对象(必须包含 id)
     * {
     *   "id": 10,
     *   "tempName": "标签A-改",
     *   "xAxis": 12
     * }
     *
     * 响应数据: { "rows": 1 }
     *
     * 示例：
     * {
     *   "id": "req_tpl_005",
     *   "path": "/business/dataPoolTemplate/update",
     *   "body": "{\"id\":10,\"tempName\":\"标签A-改\",\"xAxis\":12}"
     * }
     */

    /**
     * 6) 删除模板
     *
     * 请求路径: /business/dataPoolTemplate/delete
     *
     * 方式一(单个):
     * {
     *   "id": 10
     * }
     *
     * 方式二(批量):
     * {
     *   "ids": [10, 11, 12]
     * }
     *
     * 响应数据: { "rows": 1 } 或 { "rows": n }
     *
     * 示例：
     * 1) 单个删除
     * {
     *   "id": "req_tpl_006",
     *   "path": "/business/dataPoolTemplate/delete",
     *   "body": "{\"id\":10}"
     * }
     *
     * 2) 批量删除
     * {
     *   "id": "req_tpl_007",
     *   "path": "/business/dataPoolTemplate/delete",
     *   "body": "{\"ids\":[10,11,12]}"
     * }
     */
}


