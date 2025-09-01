package com.ruoyi.business.domain.DataPoolTemplate;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 数据池模板实体
 */
@Data
public class DataPoolTemplate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;               // 主键
    private Long poolId;           // 数据池ID
    private String tempName;       // 模板名称
    private String tempContent;    // 模板内容
    private Integer xAxis;         // x轴
    private Integer yAxis;         // y轴
    private Integer angle;         // 旋转角度
    private Integer width;         // 宽度
    private Integer height;        // 高度
    private String delFlag;        // 是否删除（0 未删除，2 已删除）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;       // 创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;       // 更新时间
}


