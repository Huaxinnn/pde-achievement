package com.meituan.pde.entity;

import lombok.Data;

import java.util.Date;

@Data
public class DiscussionLike {
    private Long id;
    private Long postId;
    private String userMis;
    private Integer deletedFlag;
    private Date addTime;
    private Date createTime;
    private Date updateTime;
}
