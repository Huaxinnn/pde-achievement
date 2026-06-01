package com.meituan.pde.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class StageStep {
    private Long id;
    private Long stageId;
    private Long sortOrder;
    private String title;
    private String description;
    private String commands; // JSON数组字符串
    private String tips;
    private Integer deletedFlag;
    private Date addTime;
    private Date createTime;
    private Date updateTime;

    // 扩展字段
    private List<String> commandList; // 解析后的命令列表
    private Boolean completed;
}
