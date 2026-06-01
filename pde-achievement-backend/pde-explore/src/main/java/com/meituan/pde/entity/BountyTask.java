package com.meituan.pde.entity;

import lombok.Data;

import java.util.Date;

@Data
public class BountyTask {
    private Long id;
    private String title;
    private String description;
    private String type;
    private String status;
    private String createdBy;
    private Date designDeadline;
    private Date votingEnd;
    private Date devDeadline;
    private Long devMinStage;
    private String rewardDesc;
    private String coverUrl;
    private String refLink;
    private Long likeCount;
    private Integer isFeatured;
    private String featuredBy;
    private String featuredReason;
    private Date featuredAt;
    private Integer deletedFlag;
    private Date addTime;
    private Date createTime;
    private Date updateTime;
}
