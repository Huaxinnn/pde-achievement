package com.meituan.pde.entity;

import lombok.Data;
import java.util.Date;

@Data
public class StageFaq {
    private Long id;
    private Long stageId;
    private String question;
    private String answer;
    private Long sortOrder;
    private Integer deletedFlag;
    private Date addTime;
    private Date createTime;
    private Date updateTime;
}
