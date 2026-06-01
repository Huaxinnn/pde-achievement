package com.meituan.pde.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Stage {
    private Long id;
    private String name;
    private String title;
    private String description;
    private String verifyType;
    private String verifyHint;
    private Long sortOrder;
    private Integer isActive;
    private Integer deletedFlag;
    private Date addTime;
    private Date createTime;
    private Date updateTime;

    // 扩展字段，不映射数据库
    private Boolean completed;
}
