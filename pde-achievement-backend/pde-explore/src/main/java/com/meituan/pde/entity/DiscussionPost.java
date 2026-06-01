package com.meituan.pde.entity;

import lombok.Data;

import java.util.Date;

@Data
public class DiscussionPost {
    private Long id;
    private Long stageId;
    private Long parentId;
    private String userMis;
    private String content;
    private Long likeCount;
    private Integer deletedFlag;
    private Date addTime;
    private Date createTime;
    private Date updateTime;
}
