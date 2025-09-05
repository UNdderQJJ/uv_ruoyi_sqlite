package com.ruoyi.business.domain.DeviceInfo.params;

import lombok.Data;

/**
 * 设备参数强类型模型（集中定义为静态内部类）。
 */
public class DeviceParams {

    @Data
    public static class SetSysTimeParam {// 设置系统时间
        /**
         * 示例 JSON:
         * {
         *   "set_systime": {"datetime": "2025-02-01 12:00:00"}
         * }
         */
        // yyyy-MM-dd HH:mm:ss
        private String datetime;// 格式：yyyy-MM-dd HH:mm:ss
    }

    @Data
    public static class ChangeObjSizeParam {// 修改指定对象的宽度和高度
        /**
         * 示例 JSON:
         * {
         *   "changeobj_size": {"objectName": "Text1", "width": 120, "height": 40}
         * }
         */
        private String objectName;// 对象名称
        private Integer width;   // 设备单位（如mm）
        private Integer height;// 设备单位（如mm）
    }

    @Data
    public static class ChangeObjPowerParam {// 设置对象功率百分比
        /**
         * 示例 JSON:
         * {
         *   "changeobj_power": {"objectName": "Logo", "powerPercent": 80}
         * }
         */
        private String objectName;// 对象名称
        private Integer powerPercent; // 0-100
    }

    @Data
    public static class ChangeObjMarknumParam {//修改指定对象的打印次数
        /**
         * 示例 JSON:
         * {
         *   "changeobj_marknum": {"objectName": "Text1", "markNum": 3}
         * }
         */
        private String objectName;// 对象名称
        private Integer markNum;// 打印次数
    }

    @Data
    public static class DitSnumParam { // 设置序列号当前值
        /**
         * 示例 JSON:
         * {
         *   "dit_snum": {"serialName": "SN1", "currentValue": 12345}
         * }
         */
        private String serialName; // 流水号名称/标识
        private Long currentValue; // 当前值
    }

    @Data
    public static class ResetSernumParam { // 重置流水号
        /**
         * 示例 JSON:
         * {
         *   "reset_sernum": {"serialName": "SN1"}
         * }
         */
        private String serialName;// 流水号名称/标识
    }

    @Data
    public static class SetFixedDataParam { // 设置以FIXED开头的固定变量
        /**
         * 示例 JSON:
         * {
         *   "setfixeddata": {"variableName": "FIXED_TITLE", "value": "Hello"}
         * }
         */
        private String variableName; // 变量名，如 FIXED xxx
        private String value;        // 内容
    }

    @Data
    public static class SetLimitCountParam { // 设置条件计数限制
        /**
         * 示例 JSON:
         * {
         *   "setlimitcount": {"maxCount": 1000, "currentCount": 0}
         * }
         */
        private Integer maxCount;    // 最大个数
        private Integer currentCount;// 当前个数
    }

    @Data
    public static class ChangeObjEnmarkParam { // 开关喷码
        /**
         * 示例 JSON:
         * {
         *   "changeobj_enmark": {"objectName": "Text1,Logo", "enabled": true}
         * }
         */
        private String objectName;   // 可用逗号分隔多个
        private Boolean enabled;     // true 开启，false 关闭
    }

    @Data
    public static class ChangeObjPosParam { // 设置对象坐标与对齐
        /**
         * 示例 JSON:
         * {
         *   "changeobj_pos": {"objectName": "Text1", "x": 10, "y": 20, "align": "left"}
         * }
         */
        private String objectName;
        private Integer x;           // 坐标X
        private Integer y;           // 坐标Y
        private String align;        // 对齐方式: left/center/right 等
    }

    @Data
    public static class ChangeTextSizeParam { // 文字对象尺寸
        /**
         * 示例 JSON:
         * {
         *   "change_textsize": {"objectName": "Text1", "height": 30, "width": 10, "charSpacing": 2}
         * }
         */
        private String objectName;
        private Integer height;
        private Integer width;
        private Integer charSpacing; // 字符间距
    }

    @Data
    public static class ChangeReplenParam { // 管线打印重复长度
        /**
         * 示例 JSON:
         * {
         *   "changereplen": {"repeatLength": 500}
         * }
         */
        private Integer repeatLength;
    }

    @Data
    public static class SetRpyModeParam { // 设备返回数据形式
        /**
         * 示例 JSON:
         * {
         *   "setrpymode": {"mode": "detail"}
         * }
         */
        private String mode; // 如: none/simple/detail 等
    }

    @Data
    public static class EditTextDataParam { // 修改对象内容
        /**
         * 示例 JSON:
         * {
         *   "edit_textdata": {"objectName": "Text1", "content": "ABC123"}
         * }
         */
        private String objectName;
        private String content;
    }

    @Data
    public static class SetaParam { // 发送打印数据（核心指令）
        /**
         * 示例 JSON:
         * {
         *   "seta": {"data": "RAW", "v1": "TEXT0001", "width": 20, "height": 10, "x": 100, "y": 50, "r": 0, "ng": 0}
         * }
         */
        private String text;    // 可选，文本变量内容
        private Integer width;  // 可选
        private Integer height; // 可选
        private Integer x;      // 可选
        private Integer y;      // 可选
        private Integer r;      // 可选，旋转角度
        private Integer ng;     // 可选，负向标志
    }

    @Data
    public static class SnumIndexQueryParam { // 获取序列号当前值
        /**
         * 示例 JSON:
         * {
         *   "snum_index": {"serialName": "SN1", "edit": 0}
         * }
         */
        private String serialName;
        private Integer edit; // 0/1
    }

    @Data
    public static class GetTextDataQueryParam { // 获取对象内容
        /**
         * 示例 JSON:
         * {
         *   "get_textdata": {"objectName": "Text1", "edit": 0}
         * }
         */
        private String objectName;
        private Integer edit; // 0/1
    }

    @Data
    public static class LoadFileParam { // 加载模板文件
        /**
         * 示例 JSON:
         * {
         *   "load": {"filePath": "folder/file1.prn"}
         * }
         */
        private String filePath;
    }

    @Data
    public static class SysStaParam { // 获取系统状态参数
        /**
         * 示例 JSON:
         * {
         *   "sys_sta": {"includeErrWarn": true}
         * }
         */
        private Boolean includeErrWarn; // true → sys_sta:errsta, false/null → sys_sta:
    }
}


