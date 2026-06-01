package com.meituan.pde.entity;

import lombok.Data;

import java.util.Date;

@Data
public class BountySubmission {
    private Long id;
    private Long taskId;
    private String phase;
    private String userMis;
    private String title;
    private String url;
    private String repoUrl;
    private String description;
    private String status;
    private String rejectReason;
    private Integer isWinner;
    private Long voteCount;
    private Integer deletedFlag;
    private Date addTime;
    private Date createTime;
    private Date updateTime;
}
