package com.ruoyi.business.enums;

/**
 * 设备参数/动作 键名枚举，对应设备通讯能力。
 */
public enum DeviceConfigKey {
    DIT_SNUM("dit_snum"),//设置序列号的当前值
    EDIT_TEXTDATA("edit_textdata"),//修改一个指定对象的内容
    SET_SYSTIME("set_systime"),//修改设备的系统时间
    CHANGEOBJ_SIZE("changeobj_size"),//修改指定对象的宽度和高度
    CHANGEOBJ_POWER("changeobj_power"),//修改指定对象的打印功率（0-100%）
    CHANGEOBJ_MARKNUM("changeobj_marknum"),//修改指定对象的打印次数
    RESET_SERNUM("reset_sernum"),//重置流水号到初始值
    SETFIXEDDATA("setfixeddata"),//设置以FIXED开头的固定变量的内容
    SETLIMITCOUNT("setlimitcount"),//设置条件计数的最大个数和当前个数，用于控制打印数量
    CHANGEOBJ_ENMARK("changeobj_enmark"),//单独设置某个或某些对象开启或关闭喷码
    CHANGEOBJ_POS("changeobj_pos"),//设置指定对象的X、Y坐标和对齐方式
    CHANGE_TEXTSIZE("change_textsize"),//修改文字对象的高度、宽度和字符间距
    CHANGEREPLEN("changereplen"),//改变管线打印的重复长度
    SETRPYMODE("setrpymode"),//设置通讯结束时，设备返回数据的形式

    // 动作与查询补全
    SETA("seta"),           // 发送打印数据
    START("start"),         // 启动加工
    STOP("stop"),           // 停止加工
    TRIMARK("trimark"),     // 软件触发打印
    SYS_STA("sys_sta"),     // 获取系统状态（可带参数 errsta）
    GETA("geta"),           // 获取缓冲区数量
    SNUM_INDEX("snum_index"), // 获取序列号当前值
    GET_TEXTDATA("get_textdata"), // 获取对象内容
    GETCOUNT("getcount"),   // 获取加工次数
    GET_CURRTEXT("get_currtext"), // 获取当前打印内容
    LOAD("load"),           // 加载模板文件
    GET_FILELIST("get_filelist"), // 获取文件列表
    CLEARBUF("clearbuf"),   // 清空数据缓冲区
    PI_CLOSEUV("pi_closeuv"); // 一键关机（PI激光机）

    private final String key;

    DeviceConfigKey(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static DeviceConfigKey fromKey(String key) {
        for (DeviceConfigKey k : values()) {
            if (k.key.equalsIgnoreCase(key)) {
                return k;
            }
        }
        return null;
    }
}


