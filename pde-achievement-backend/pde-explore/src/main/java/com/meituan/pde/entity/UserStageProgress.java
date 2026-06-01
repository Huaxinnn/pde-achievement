package com.meituan.pde.entity;

import lombok.Data;
import java.util.Date;

@Data
public class UserStageProgress {
    private Long id;
    private String userMis;
    private String userOrg;
    private String userOrgId;
    private Long stageId;
    private String verifyStatus; // pending / passed / failed
    private String submittedValue;
    private Date passedAt;
    private String lane;
    private String url;
    private Date reviewedAt;
    private String reviewedBy;
    private String rejectReason;
    private Integer deletedFlag;
    private Date addTime;
    private Date createTime;
    private Date updateTime;
}
