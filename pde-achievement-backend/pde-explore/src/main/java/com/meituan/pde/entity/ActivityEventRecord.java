package com.meituan.pde.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ActivityEventRecord {
    private Long id;
    private String userMis;
    private String userName;
    private String eventType;
    private Long stageId;
    private String stageName;
    private Date occurredAt;
    private Integer deletedFlag;
    private Date addTime;
    private Date updateTime;
}
