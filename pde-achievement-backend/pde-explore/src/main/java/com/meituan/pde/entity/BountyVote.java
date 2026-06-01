package com.meituan.pde.entity;

import lombok.Data;

import java.util.Date;

@Data
public class BountyVote {
    private Long id;
    private Long taskId;
    private Long submissionId;
    private String userMis;
    private Integer deletedFlag;
    private Date addTime;
    private Date createTime;
    private Date updateTime;
}
